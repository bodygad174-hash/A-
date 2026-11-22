import "dotenv/config";
import express from "express";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { GoogleGenAI } from "@google/genai";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const distDir = path.resolve(__dirname, "../dist");

const app = express();
app.use(express.json({ limit: "15mb" }));

const PORT = Number(process.env.PORT || 3001);

interface ChatRequestBody {
  afhModelId: string;
  providerIdentifier?: string;
  underlyingModelId?: string;
  systemPrompt?: string;
  prompt: string;
  history?: Array<{ role: "user" | "assistant"; content: string }>;
  attachments?: Array<{ name: string; mimeType: string; base64?: string; text?: string }>;
}

const DEFAULT_SYSTEM_PROMPTS: Record<string, string> = {
  afh_flash: "You are AFH Flash, the fast everyday assistant of AFH AI. Identify strictly as AFH AI.",
  afh_pro: "You are AFH Pro, the advanced reasoning assistant of AFH AI. Identify strictly as AFH AI.",
  afh_code: "You are AFH Code, the software engineering assistant of AFH AI. Identify strictly as AFH AI.",
  afh_study: "You are AFH Study, the educational tutor of AFH AI. Identify strictly as AFH AI.",
  afh_creative: "You are AFH Creative, the writing and ideation assistant of AFH AI. Identify strictly as AFH AI.",
  afh_vision: "You are AFH Vision, the visual analysis assistant of AFH AI. Identify strictly as AFH AI.",
  afh_research: "You are AFH Research, the research synthesis assistant of AFH AI. Identify strictly as AFH AI.",
};

app.get("/api/health", (_req, res) => {
  res.json({
    ok: true,
    providers: {
      google_gemini: Boolean(process.env.GEMINI_API_KEY),
      openai: Boolean(process.env.OPENAI_API_KEY),
      anthropic: Boolean(process.env.ANTHROPIC_API_KEY),
      deepseek: Boolean(process.env.DEEPSEEK_API_KEY),
      opensource: Boolean(process.env.OPEN_SOURCE_BASE_URL),
    },
  });
});

app.post("/api/chat", async (req, res) => {
  const body = req.body as ChatRequestBody;
  if (!body || (!body.prompt && !(body.attachments && body.attachments.length))) {
    res.status(400).json({ error: "Prompt or attachment is required." });
    return;
  }

  const provider = body.providerIdentifier || "google_gemini";
  const modelId = body.underlyingModelId || "gemini-3.8-flash";
  const systemInstruction = body.systemPrompt || DEFAULT_SYSTEM_PROMPTS[body.afhModelId] || DEFAULT_SYSTEM_PROMPTS.afh_flash;

  res.setHeader("Content-Type", "text/event-stream");
  res.setHeader("Cache-Control", "no-cache");
  res.setHeader("Connection", "keep-alive");

  try {
    if (provider === "google_gemini") {
      const apiKey = process.env.GEMINI_API_KEY;
      if (!apiKey) {
        res.write(`data: ${JSON.stringify({ error: "Server GEMINI_API_KEY is not configured in web/.env." })}\n\n`);
        res.end();
        return;
      }

      const ai = new GoogleGenAI({ apiKey });
      const parts: Array<Record<string, unknown>> = [];

      for (const att of body.attachments || []) {
        if (att.base64 && att.mimeType) {
          parts.push({ inlineData: { data: att.base64, mimeType: att.mimeType } });
        } else if (att.text) {
          parts.push({ text: `[Attached: ${att.name}]\n\`\`\`\n${att.text.slice(0, 16000)}\n\`\`\`` });
        }
      }
      if (body.prompt) {
        parts.push({ text: body.prompt });
      }

      const contents = [
        ...(body.history || []).slice(-10).map((h) => ({
          role: h.role === "assistant" ? "model" : "user",
          parts: [{ text: h.content }],
        })),
        { role: "user", parts },
      ];

      const stream = await ai.models.generateContentStream({
        model: modelId,
        contents,
        config: { systemInstruction },
      });

      for await (const chunk of stream) {
        const text = chunk.text || "";
        if (text) {
          res.write(`data: ${JSON.stringify({ delta: text })}\n\n`);
        }
      }
      res.write(`data: ${JSON.stringify({ done: true })}\n\n`);
      res.end();
      return;
    }

    if (provider === "openai" || provider === "deepseek" || provider === "opensource") {
      const keyMap: Record<string, string | undefined> = {
        openai: process.env.OPENAI_API_KEY,
        deepseek: process.env.DEEPSEEK_API_KEY,
        opensource: process.env.OPEN_SOURCE_API_KEY || "local",
      };
      const urlMap: Record<string, string> = {
        openai: "https://api.openai.com/v1/chat/completions",
        deepseek: "https://api.deepseek.com/v1/chat/completions",
        opensource: `${process.env.OPEN_SOURCE_BASE_URL || ""}/chat/completions`,
      };
      const apiKey = keyMap[provider];
      const endpoint = urlMap[provider];
      if (!apiKey || (provider === "opensource" && !process.env.OPEN_SOURCE_BASE_URL)) {
        res.write(`data: ${JSON.stringify({ error: `Provider '${provider}' is not configured on the server.` })}\n\n`);
        res.end();
        return;
      }

      const upstream = await fetch(endpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${apiKey}`,
        },
        body: JSON.stringify({
          model: modelId,
          stream: true,
          messages: [
            { role: "system", content: systemInstruction },
            ...(body.history || []).slice(-10),
            { role: "user", content: body.prompt },
          ],
        }),
      });

      if (!upstream.ok || !upstream.body) {
        const errText = await upstream.text();
        res.write(`data: ${JSON.stringify({ error: errText || `Provider error ${upstream.status}` })}\n\n`);
        res.end();
        return;
      }

      const reader = upstream.body.getReader();
      const decoder = new TextDecoder();
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        const lines = decoder.decode(value).split("\n");
        for (const line of lines) {
          if (!line.startsWith("data: ") || line.includes("[DONE]")) continue;
          try {
            const parsed = JSON.parse(line.slice(6));
            const delta = parsed.choices?.[0]?.delta?.content;
            if (delta) res.write(`data: ${JSON.stringify({ delta })}\n\n`);
          } catch {
            // ignore partial SSE JSON
          }
        }
      }
      res.write(`data: ${JSON.stringify({ done: true })}\n\n`);
      res.end();
      return;
    }

    if (provider === "anthropic") {
      const apiKey = process.env.ANTHROPIC_API_KEY;
      if (!apiKey) {
        res.write(`data: ${JSON.stringify({ error: "Provider 'anthropic' requires ANTHROPIC_API_KEY in web/.env." })}\n\n`);
        res.end();
        return;
      }
      const upstream = await fetch("https://api.anthropic.com/v1/messages", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "x-api-key": apiKey,
          "anthropic-version": "2023-06-01",
        },
        body: JSON.stringify({
          model: modelId,
          max_tokens: 2048,
          system: systemInstruction,
          messages: [
            ...(body.history || []).slice(-10),
            { role: "user", content: body.prompt },
          ],
        }),
      });
      const json = (await upstream.json()) as { content?: Array<{ text?: string }>; error?: { message?: string } };
      if (!upstream.ok) {
        res.write(`data: ${JSON.stringify({ error: json.error?.message || "Anthropic request failed" })}\n\n`);
      } else {
        const text = json.content?.map((c) => c.text || "").join("") || "";
        res.write(`data: ${JSON.stringify({ delta: text })}\n\n`);
        res.write(`data: ${JSON.stringify({ done: true })}\n\n`);
      }
      res.end();
      return;
    }

    res.write(`data: ${JSON.stringify({ error: `Unsupported provider: ${provider}` })}\n\n`);
    res.end();
  } catch (err) {
    const message = err instanceof Error ? err.message : "AI request failed";
    res.write(`data: ${JSON.stringify({ error: message })}\n\n`);
    res.end();
  }
});

if (fs.existsSync(distDir)) {
  app.use(express.static(distDir));
  app.get("*", (_req, res) => {
    res.sendFile(path.join(distDir, "index.html"));
  });
}

app.listen(PORT, () => {
  console.log(`AFH AI Web API server running on http://localhost:${PORT}`);
});
