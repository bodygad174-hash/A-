export type Language = "en" | "ar";
export type ThemeMode = "dark" | "light" | "system";

export interface UserProfile {
  userId: string;
  name: string;
  email: string;
  avatarUrl?: string;
  role: "user" | "admin";
  status: "active" | "suspended";
  dailyRequestsUsed: number;
  monthlyRequestsUsed: number;
  dailyLimit: number;
  monthlyLimit: number;
  preferredLanguage: Language;
  preferredTheme: ThemeMode;
  defaultAfhModelId: string;
  streamingEnabled: boolean;
}

export interface Conversation {
  id: string;
  userId: string;
  title: string;
  selectedAfhModelId: string;
  favorite: boolean;
  lastMessagePreview: string;
  messageCount: number;
  createdAt?: unknown;
  updatedAt?: unknown;
}

export interface ChatMessage {
  id: string;
  userId: string;
  conversationId: string;
  role: "user" | "assistant";
  content: string;
  afhModelId: string;
  attachmentNames: string[];
  attachmentTypes: string[];
  errorMessage?: string;
  createdAt?: { toDate?: () => Date } | null;
}

export interface AfhModelSpec {
  id: string;
  afhModelName: string;
  afhModelNameAr: string;
  description: string;
  descriptionAr: string;
  iconKey: string;
  capabilities: string[];
  supportedFileTypes: string[];
  tierType: "free" | "limited" | "pro";
  enabled: boolean;
  dailyLimit: number;
  monthlyLimit: number;
  sortOrder: number;
}

export interface AdminProviderMapping {
  afhModelId: string;
  providerIdentifier: "google_gemini" | "openai" | "anthropic" | "deepseek" | "opensource";
  underlyingModelId: string;
  systemPrompt: string;
  rateLimitRpm: number;
  timeoutSeconds: number;
}

export interface ProviderSetting {
  providerIdentifier: string;
  displayName: string;
  enabled: boolean;
  configMode: "firebase_app_check" | "server_env";
  status: "connected" | "unconfigured" | "disabled";
  rateLimitRpm: number;
  notes: string;
}

export interface UsageLog {
  id: string;
  userId: string;
  afhModelId: string;
  status: "success" | "error" | "quota_exceeded";
  latencyMs: number;
  errorCode?: string;
  createdAt?: { toDate?: () => Date } | null;
}

export const DEFAULT_AFH_MODELS: AfhModelSpec[] = [
  {
    id: "afh_flash",
    afhModelName: "AFH Flash",
    afhModelNameAr: "AFH فلاش",
    description: "Fast answers and everyday assistance.",
    descriptionAr: "إجابات سريعة ومساعدة يومية ذكية.",
    iconKey: "flash",
    capabilities: ["Streaming", "Fast Chat", "Vision", "Files"],
    supportedFileTypes: ["PNG", "JPG", "TXT", "MD", "PDF"],
    tierType: "free",
    enabled: true,
    dailyLimit: 100,
    monthlyLimit: 2500,
    sortOrder: 1,
  },
  {
    id: "afh_pro",
    afhModelName: "AFH Pro",
    afhModelNameAr: "AFH برو",
    description: "Advanced reasoning, analysis, and problem solving.",
    descriptionAr: "تفكير متقدم وتحليل عميق وحل المشكلات المعقدة.",
    iconKey: "pro",
    capabilities: ["Streaming", "Deep Reasoning", "Vision", "Files"],
    supportedFileTypes: ["PNG", "JPG", "PDF", "TXT", "JSON"],
    tierType: "limited",
    enabled: true,
    dailyLimit: 40,
    monthlyLimit: 800,
    sortOrder: 2,
  },
  {
    id: "afh_code",
    afhModelName: "AFH Code",
    afhModelNameAr: "AFH كود",
    description: "Programming, debugging, and software development.",
    descriptionAr: "البرمجة وتصحيح الأخطاء وتطوير البرمجيات.",
    iconKey: "code",
    capabilities: ["Streaming", "Code Architecture", "Files"],
    supportedFileTypes: ["KT", "PY", "JS", "TS", "JSON", "TXT", "MD"],
    tierType: "free",
    enabled: true,
    dailyLimit: 80,
    monthlyLimit: 2000,
    sortOrder: 3,
  },
  {
    id: "afh_study",
    afhModelName: "AFH Study",
    afhModelNameAr: "AFH للدراسة",
    description: "Education, explanations, studying, and homework assistance.",
    descriptionAr: "التعليم والشرح المبسط والمذاكرة وحل الواجبات.",
    iconKey: "study",
    capabilities: ["Streaming", "Step-by-Step Tutor", "Vision", "Files"],
    supportedFileTypes: ["PNG", "JPG", "PDF", "TXT"],
    tierType: "free",
    enabled: true,
    dailyLimit: 80,
    monthlyLimit: 2000,
    sortOrder: 4,
  },
  {
    id: "afh_creative",
    afhModelName: "AFH Creative",
    afhModelNameAr: "AFH الإبداعي",
    description: "Writing, ideas, creativity, and content & visual generation.",
    descriptionAr: "الكتابة والأفكار الإبداعية وتوليد المحتوى والصور.",
    iconKey: "creative",
    capabilities: ["Streaming", "Copywriting", "Image Studio", "Vision"],
    supportedFileTypes: ["PNG", "JPG", "TXT", "MD"],
    tierType: "limited",
    enabled: true,
    dailyLimit: 30,
    monthlyLimit: 600,
    sortOrder: 5,
  },
  {
    id: "afh_vision",
    afhModelName: "AFH Vision",
    afhModelNameAr: "AFH البصري",
    description: "Image understanding and visual analysis, when supported.",
    descriptionAr: "فهم الصور والتحليل البصري المتقدم.",
    iconKey: "vision",
    capabilities: ["Streaming", "Vision", "Visual Q&A", "OCR"],
    supportedFileTypes: ["PNG", "JPG", "WEBP"],
    tierType: "free",
    enabled: true,
    dailyLimit: 50,
    monthlyLimit: 1200,
    sortOrder: 6,
  },
  {
    id: "afh_research",
    afhModelName: "AFH Research",
    afhModelNameAr: "AFH للأبحاث",
    description: "Research, information analysis, and long-form answers.",
    descriptionAr: "البحث العميق وتحليل المعلومات والإجابات المطولة.",
    iconKey: "research",
    capabilities: ["Streaming", "Deep Reasoning", "PDF", "Files"],
    supportedFileTypes: ["PDF", "TXT", "MD", "CSV", "JSON"],
    tierType: "limited",
    enabled: true,
    dailyLimit: 35,
    monthlyLimit: 700,
    sortOrder: 7,
  },
];

export const BOOTSTRAP_ADMIN_EMAIL = "bodygad999@gmail.com";
