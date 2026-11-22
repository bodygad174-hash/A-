import React, { useEffect, useMemo, useRef, useState } from "react";
import { onAuthStateChanged, signInWithPopup, signOut, User } from "firebase/auth";
import {
  addDoc,
  collection,
  doc,
  getDoc,
  getDocs,
  increment,
  limit,
  onSnapshot,
  orderBy,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  writeBatch,
} from "firebase/firestore";
import {
  Bot,
  Code2,
  Copy,
  Edit3,
  Eye,
  FileText,
  FlaskConical,
  Globe,
  Home,
  LogOut,
  Menu,
  MessageSquare,
  Moon,
  Palette,
  Paperclip,
  Plus,
  RefreshCw,
  Search,
  Send,
  Settings,
  Shield,
  Sparkles,
  Square,
  Star,
  Sun,
  Trash2,
  X,
  Zap,
} from "lucide-react";
import { auth, db, googleProvider } from "./firebase";
import {
  AdminProviderMapping,
  AfhModelSpec,
  BOOTSTRAP_ADMIN_EMAIL,
  ChatMessage,
  Conversation,
  DEFAULT_AFH_MODELS,
  Language,
  ProviderSetting,
  UsageLog,
  UserProfile,
} from "./models";

type View = "home" | "chat" | "settings" | "admin";

function ModelIcon({ iconKey }: { iconKey: string }) {
  switch (iconKey) {
    case "flash":
      return <Zap className="w-4 h-4 text-[#00D5FF]" />;
    case "pro":
      return <Sparkles className="w-4 h-4 text-[#1677FF]" />;
    case "code":
      return <Code2 className="w-4 h-4 text-[#00E6C7]" />;
    case "study":
      return <Bot className="w-4 h-4 text-[#47B5FF]" />;
    case "creative":
      return <Palette className="w-4 h-4 text-[#875BFF]" />;
    case "vision":
      return <Eye className="w-4 h-4 text-[#28D8E6]" />;
    case "research":
      return <FlaskConical className="w-4 h-4 text-[#7B88FF]" />;
    default:
      return <Sparkles className="w-4 h-4 text-[#00D5FF]" />;
  }
}

export default function App() {
  const [user, setUser] = useState<User | null>(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [view, setView] = useState<View>("home");
  const [language, setLanguage] = useState<Language>("en");
  const [theme, setTheme] = useState<"dark" | "light">("dark");
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const [models, setModels] = useState<AfhModelSpec[]>(DEFAULT_AFH_MODELS);
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [activeConvId, setActiveConvId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [selectedModelId, setSelectedModelId] = useState("afh_flash");

  const [searchQuery, setSearchQuery] = useState("");
  const [favoritesOnly, setFavoritesOnly] = useState(false);
  const [inputText, setInputText] = useState("");
  const [isStreaming, setIsStreaming] = useState(false);
  const [streamingContent, setStreamingContent] = useState("");
  const [notice, setNotice] = useState<string | null>(null);
  const [errorBanner, setErrorBanner] = useState<string | null>(null);
  const [attachments, setAttachments] = useState<
    Array<{ name: string; mimeType: string; base64?: string; text?: string }>
  >([]);

  // Admin state (only loaded for admins)
  const [adminUsers, setAdminUsers] = useState<UserProfile[]>([]);
  const [adminMappings, setAdminMappings] = useState<Record<string, AdminProviderMapping>>({});
  const [providers, setProviders] = useState<ProviderSetting[]>([]);
  const [usageLogs, setUsageLogs] = useState<UsageLog[]>([]);

  const abortRef = useRef<AbortController | null>(null);
  const isArabic = language === "ar";

  useEffect(() => {
    document.documentElement.dir = isArabic ? "rtl" : "ltr";
    document.documentElement.lang = language;
  }, [isArabic, language]);

  // Auth listener & user profile sync (shared with Android app!)
  useEffect(() => {
    return onAuthStateChanged(auth, async (u) => {
      setUser(u);
      setAuthLoading(false);
      if (!u) {
        setProfile(null);
        setConversations([]);
        setMessages([]);
        return;
      }

      const userRef = doc(db, "users", u.uid);
      const snap = await getDoc(userRef);
      if (!snap.exists()) {
        const isBootAdmin = u.email?.toLowerCase() === BOOTSTRAP_ADMIN_EMAIL && u.emailVerified;
        await setDoc(userRef, {
          userId: u.uid,
          name: u.displayName || "AFH User",
          email: u.email || "",
          ...(u.photoURL ? { avatarUrl: u.photoURL } : {}),
          role: isBootAdmin ? "admin" : "user",
          status: "active",
          dailyRequestsUsed: 0,
          monthlyRequestsUsed: 0,
          dailyLimit: 50,
          monthlyLimit: 1000,
          preferredLanguage: "en",
          preferredTheme: "dark",
          defaultAfhModelId: "afh_flash",
          streamingEnabled: true,
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp(),
        });
      }
    });
  }, []);

  // Real-time Firestore listeners (1:1 synced with Android!)
  useEffect(() => {
    if (!user) return;
    const unsubProfile = onSnapshot(doc(db, "users", user.uid), (snap) => {
      if (snap.exists()) {
        const data = snap.data() as UserProfile;
        setProfile(data);
        setLanguage(data.preferredLanguage === "ar" ? "ar" : "en");
        if (data.preferredTheme === "light" || data.preferredTheme === "dark") {
          setTheme(data.preferredTheme);
        }
      }
    });

    const unsubModels = onSnapshot(
      query(collection(db, "ai_models"), orderBy("sortOrder", "asc")),
      (snap) => {
        if (!snap.empty) {
          setModels(snap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<AfhModelSpec, "id">) })));
        }
      }
    );

    const unsubConvs = onSnapshot(
      query(collection(db, "users", user.uid, "conversations"), orderBy("updatedAt", "desc")),
      (snap) => {
        setConversations(snap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<Conversation, "id">) })));
      }
    );

    return () => {
      unsubProfile();
      unsubModels();
      unsubConvs();
    };
  }, [user]);

  useEffect(() => {
    if (!user || !activeConvId) {
      setMessages([]);
      return;
    }
    return onSnapshot(
      query(
        collection(db, "users", user.uid, "conversations", activeConvId, "messages"),
        orderBy("createdAt", "asc")
      ),
      (snap) => {
        setMessages(snap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<ChatMessage, "id">) })));
      }
    );
  }, [user, activeConvId]);

  const isAdmin =
    profile?.role === "admin" ||
    (user?.email?.toLowerCase() === BOOTSTRAP_ADMIN_EMAIL && user?.emailVerified);

  useEffect(() => {
    if (!user || !isAdmin) return;
    const u1 = onSnapshot(collection(db, "users"), (snap) => {
      setAdminUsers(snap.docs.map((d) => d.data() as UserProfile));
    });
    const u2 = onSnapshot(collection(db, "admin_provider_mappings"), (snap) => {
      const map: Record<string, AdminProviderMapping> = {};
      snap.docs.forEach((d) => {
        map[d.id] = d.data() as AdminProviderMapping;
      });
      setAdminMappings(map);
    });
    const u3 = onSnapshot(collection(db, "provider_settings"), (snap) => {
      setProviders(snap.docs.map((d) => d.data() as ProviderSetting));
    });
    const u4 = onSnapshot(
      query(collection(db, "usage_logs"), orderBy("createdAt", "desc"), limit(30)),
      (snap) => {
        setUsageLogs(snap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<UsageLog, "id">) })));
      }
    );
    return () => {
      u1();
      u2();
      u3();
      u4();
    };
  }, [user, isAdmin]);

  const activeModel = useMemo(
    () => models.find((m) => m.id === selectedModelId) || models[0],
    [models, selectedModelId]
  );

  const filteredConversations = useMemo(
    () =>
      conversations.filter((c) => {
        const q = searchQuery.toLowerCase();
        const matchesQ = !q || c.title.toLowerCase().includes(q) || c.lastMessagePreview.toLowerCase().includes(q);
        const matchesFav = !favoritesOnly || c.favorite;
        return matchesQ && matchesFav;
      }),
    [conversations, searchQuery, favoritesOnly]
  );

  async function handleSignIn() {
    setErrorBanner(null);
    try {
      await signInWithPopup(auth, googleProvider);
      setView("chat");
    } catch (err) {
      setErrorBanner(err instanceof Error ? err.message : "Google Sign-In failed");
    }
  }

  async function handleSend(overridePrompt?: string) {
    if (!user) {
      await handleSignIn();
      return;
    }
    const prompt = (overridePrompt ?? inputText).trim();
    if ((!prompt && attachments.length === 0) || isStreaming) return;
    if (profile?.status === "suspended") {
      setErrorBanner(isArabic ? "تم إيقاف حسابك مؤقتاً. تواصل مع الإدارة." : "Your account is suspended. Contact an administrator.");
      return;
    }
    if (!activeModel?.enabled) {
      setErrorBanner(`${activeModel?.afhModelName} is currently disabled.`);
      return;
    }
    if (profile && (profile.dailyRequestsUsed >= profile.dailyLimit || profile.monthlyRequestsUsed >= profile.monthlyLimit)) {
      setErrorBanner("Daily or monthly quota reached.");
      return;
    }

    setErrorBanner(null);
    if (!overridePrompt) setInputText("");
    const currentAttachments = [...attachments];
    setAttachments([]);

    let convId = activeConvId;
    if (!convId) {
      const convRef = await addDoc(collection(db, "users", user.uid, "conversations"), {
        userId: user.uid,
        title: (prompt || currentAttachments[0]?.name || "New Chat").slice(0, 60),
        selectedAfhModelId: activeModel.id,
        favorite: false,
        lastMessagePreview: "",
        messageCount: 0,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      convId = convRef.id;
      setActiveConvId(convId);
    }

    await addDoc(collection(db, "users", user.uid, "conversations", convId, "messages"), {
      userId: user.uid,
      conversationId: convId,
      role: "user",
      content: prompt || `[Attached ${currentAttachments.length} file(s)]`,
      afhModelId: activeModel.id,
      attachmentNames: currentAttachments.map((a) => a.name),
      attachmentTypes: currentAttachments.map((a) => a.mimeType),
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });

    setIsStreaming(true);
    setStreamingContent("");
    const controller = new AbortController();
    abortRef.current = controller;
    const startMs = Date.now();

    try {
      const mapping = adminMappings[activeModel.id];
      const res = await fetch("/api/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        signal: controller.signal,
        body: JSON.stringify({
          afhModelId: activeModel.id,
          providerIdentifier: mapping?.providerIdentifier,
          underlyingModelId: mapping?.underlyingModelId,
          systemPrompt: mapping?.systemPrompt,
          prompt,
          history: messages.map((m) => ({ role: m.role, content: m.content })),
          attachments: currentAttachments,
        }),
      });

      const reader = res.body?.getReader();
      if (!reader) throw new Error("Streaming unavailable.");
      const decoder = new TextDecoder();
      let fullText = "";

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        const lines = decoder.decode(value).split("\n");
        for (const line of lines) {
          if (!line.startsWith("data: ")) continue;
          const payload = JSON.parse(line.slice(6));
          if (payload.error) throw new Error(payload.error);
          if (payload.delta) {
            fullText += payload.delta;
            setStreamingContent(fullText);
          }
        }
      }

      await addDoc(collection(db, "users", user.uid, "conversations", convId, "messages"), {
        userId: user.uid,
        conversationId: convId,
        role: "assistant",
        content: fullText,
        afhModelId: activeModel.id,
        attachmentNames: [],
        attachmentTypes: [],
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });

      await updateDoc(doc(db, "users", user.uid, "conversations", convId), {
        lastMessagePreview: fullText.slice(0, 120),
        messageCount: increment(2),
        updatedAt: serverTimestamp(),
      });

      await updateDoc(doc(db, "users", user.uid), {
        dailyRequestsUsed: increment(1),
        monthlyRequestsUsed: increment(1),
        updatedAt: serverTimestamp(),
      });

      await addDoc(collection(db, "usage_logs"), {
        userId: user.uid,
        afhModelId: activeModel.id,
        status: "success",
        latencyMs: Math.max(0, Date.now() - startMs),
        createdAt: serverTimestamp(),
      });
    } catch (err) {
      if ((err as Error).name !== "AbortError") {
        const msg = err instanceof Error ? err.message : "Failed to get response";
        setErrorBanner(msg);
        await addDoc(collection(db, "usage_logs"), {
          userId: user.uid,
          afhModelId: activeModel.id,
          status: "error",
          latencyMs: Math.max(0, Date.now() - startMs),
          errorCode: msg.slice(0, 100),
          createdAt: serverTimestamp(),
        }).catch(() => {});
      }
    } finally {
      setIsStreaming(false);
      setStreamingContent("");
      abortRef.current = null;
    }
  }

  function handleFileSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    if (file.type.startsWith("image/") || file.type === "application/pdf") {
      reader.onload = () => {
        const base64 = String(reader.result || "").split(",")[1];
        setAttachments((prev) => [...prev.slice(0, 4), { name: file.name, mimeType: file.type, base64 }]);
      };
      reader.readAsDataURL(file);
    } else {
      reader.onload = () => {
        setAttachments((prev) => [
          ...prev.slice(0, 4),
          { name: file.name, mimeType: file.type || "text/plain", text: String(reader.result || "") },
        ]);
      };
      reader.readAsText(file);
    }
  }

  const rootBg = theme === "dark" ? "bg-[#080D19] text-[#F5F8FF]" : "bg-[#F5F8FF] text-[#080D19]";
  const cardBg =
    theme === "dark"
      ? "bg-[#101A30]/85 border-[#23355C] backdrop-blur-xl"
      : "bg-white/90 border-[#D0DEF5] backdrop-blur-xl";

  return (
    <div className={`min-h-screen flex flex-col ${rootBg}`}>
      {/* Top Navbar */}
      <header className={`sticky top-0 z-40 border-b ${cardBg} px-4 py-3 flex items-center justify-between`}>
        <div className="flex items-center gap-3">
          {user && (
            <button
              onClick={() => setSidebarOpen(!sidebarOpen)}
              className="lg:hidden p-2 rounded-xl hover:bg-[#1677FF]/20"
              aria-label="Toggle Sidebar"
            >
              <Menu className="w-5 h-5" />
            </button>
          )}
          <button onClick={() => setView("home")} className="flex items-center gap-2.5 text-left">
            <img
              src="/afh-logo.jpg"
              alt="AFH AI Logo"
              className="w-9 h-9 rounded-xl object-cover border border-[#00D5FF]/50"
            />
            <div>
              <span className="font-display font-bold text-lg tracking-wide">
                AFH <span className="text-[#00D5FF]">AI</span>
              </span>
              <p className="text-[11px] opacity-70 hidden sm:block">
                {isArabic ? "كل الذكاء الاصطناعي في مكان واحد" : "One Platform. Your AI. Your Identity."}
              </p>
            </div>
          </button>
        </div>

        <nav className="flex items-center gap-2">
          <button
            onClick={() => setView("home")}
            className={`px-3 py-1.5 rounded-xl text-sm flex items-center gap-1.5 ${
              view === "home" ? "bg-[#1677FF] text-white" : "hover:bg-[#1677FF]/15"
            }`}
          >
            <Home className="w-4 h-4" />
            <span className="hidden sm:inline">{isArabic ? "الرئيسية" : "Home"}</span>
          </button>
          <button
            onClick={() => setView("chat")}
            className={`px-3 py-1.5 rounded-xl text-sm flex items-center gap-1.5 ${
              view === "chat" ? "bg-[#1677FF] text-white" : "hover:bg-[#1677FF]/15"
            }`}
          >
            <MessageSquare className="w-4 h-4" />
            <span className="hidden sm:inline">{isArabic ? "المحادثة" : "Chat"}</span>
          </button>
          {user && (
            <button
              onClick={() => setView("settings")}
              className={`px-3 py-1.5 rounded-xl text-sm flex items-center gap-1.5 ${
                view === "settings" ? "bg-[#1677FF] text-white" : "hover:bg-[#1677FF]/15"
              }`}
            >
              <Settings className="w-4 h-4" />
              <span className="hidden sm:inline">{isArabic ? "الإعدادات" : "Settings"}</span>
            </button>
          )}
          {isAdmin && (
            <button
              onClick={() => setView("admin")}
              className={`px-3 py-1.5 rounded-xl text-sm flex items-center gap-1.5 ${
                view === "admin" ? "bg-[#00E6C7] text-[#080D19] font-semibold" : "hover:bg-[#00E6C7]/15 text-[#00E6C7]"
              }`}
            >
              <Shield className="w-4 h-4" />
              <span className="hidden sm:inline">{isArabic ? "الإدارة" : "Admin"}</span>
            </button>
          )}
          <button
            onClick={async () => {
              const next = isArabic ? "en" : "ar";
              setLanguage(next);
              if (user) {
                await updateDoc(doc(db, "users", user.uid), {
                  preferredLanguage: next,
                  updatedAt: serverTimestamp(),
                }).catch(() => {});
              }
            }}
            className="p-2 rounded-xl hover:bg-[#1677FF]/15"
            title="Switch Language"
          >
            <Globe className="w-4 h-4 text-[#00D5FF]" />
          </button>
          <button
            onClick={async () => {
              const next = theme === "dark" ? "light" : "dark";
              setTheme(next);
              if (user) {
                await updateDoc(doc(db, "users", user.uid), {
                  preferredTheme: next,
                  updatedAt: serverTimestamp(),
                }).catch(() => {});
              }
            }}
            className="p-2 rounded-xl hover:bg-[#1677FF]/15"
            title="Toggle Theme"
          >
            {theme === "dark" ? <Sun className="w-4 h-4 text-[#00E6C7]" /> : <Moon className="w-4 h-4" />}
          </button>
          {!user && !authLoading && (
            <button
              onClick={handleSignIn}
              className="px-4 py-2 rounded-xl bg-[#1677FF] text-white text-sm font-semibold hover:bg-[#1677FF]/90"
            >
              {isArabic ? "تسجيل الدخول" : "Sign In"}
            </button>
          )}
        </nav>
      </header>

      {/* Main Content */}
      {view === "home" && (
        <main className="flex-1 max-w-6xl mx-auto w-full px-4 py-10 space-y-14">
          <section className={`rounded-3xl border p-8 md:p-12 ${cardBg} grid md:grid-cols-2 gap-8 items-center`}>
            <div className="space-y-5">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#1677FF]/20 text-[#00D5FF] text-xs font-semibold">
                <Sparkles className="w-3.5 h-3.5" />
                {isArabic ? "مزامنة مباشرة مع تطبيق Android" : "Synced Live with AFH AI Android App"}
              </div>
              <h1 className="font-display text-3xl md:text-5xl font-bold leading-tight">
                {isArabic ? "ذكاؤك الاصطناعي.. كله في مكان واحد" : "Your Intelligence. One Platform."}
              </h1>
              <p className="text-base opacity-80">
                {isArabic
                  ? "استكشف طريقة أذكى للتفكير والإبداع والتعلم والبناء عبر منصة AFH AI الموحدة بين الويب والهاتف."
                  : "Explore a smarter way to think, create, learn, and build with AFH AI — seamlessly synced across Web and Mobile."}
              </p>
              <div className="flex flex-wrap gap-3">
                <button
                  onClick={() => (user ? setView("chat") : handleSignIn())}
                  className="px-6 py-3 rounded-2xl bg-[#1677FF] text-white font-semibold hover:bg-[#1677FF]/90"
                >
                  {isArabic ? "ابدأ المحادثة" : "Start Chatting"}
                </button>
                <a
                  href="#models"
                  className="px-6 py-3 rounded-2xl border border-[#00D5FF]/50 text-[#00D5FF] font-semibold hover:bg-[#00D5FF]/10"
                >
                  {isArabic ? "استكشف النماذج" : "Explore AFH AI"}
                </a>
              </div>
            </div>
            <div className="flex justify-center">
              <img
                src="/afh-logo.jpg"
                alt="AFH AI Official Logo"
                className="w-full max-w-sm rounded-3xl border border-[#00D5FF]/40 shadow-2xl"
              />
            </div>
          </section>

          <section id="models" className="space-y-6">
            <h2 className="font-display text-2xl md:text-3xl font-bold">
              {isArabic ? "نماذج AFH AI الحصرية" : "AFH AI Branded Models"}
            </h2>
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {models.map((m) => (
                <div
                  key={m.id}
                  onClick={() => {
                    if (!m.enabled) return;
                    setSelectedModelId(m.id);
                    if (user) setView("chat");
                    else handleSignIn();
                  }}
                  className={`rounded-2xl border p-5 cursor-pointer transition hover:border-[#00D5FF] ${cardBg}`}
                >
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-2.5">
                      <ModelIcon iconKey={m.iconKey} />
                      <h3 className="font-display font-bold">{isArabic ? m.afhModelNameAr : m.afhModelName}</h3>
                    </div>
                    <span className="text-xs px-2.5 py-0.5 rounded-full bg-[#00E6C7]/15 text-[#00E6C7]">
                      {m.enabled ? m.tierType.toUpperCase() : "DISABLED"}
                    </span>
                  </div>
                  <p className="text-sm opacity-80 mb-3">{isArabic ? m.descriptionAr : m.description}</p>
                  <div className="flex flex-wrap gap-1.5">
                    {m.capabilities.map((c) => (
                      <span key={c} className="text-[11px] px-2 py-0.5 rounded-lg bg-[#1677FF]/15 text-[#00D5FF]">
                        {c}
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </section>
        </main>
      )}

      {view === "chat" && (
        <div className="flex-1 flex overflow-hidden">
          {/* Sidebar */}
          <aside
            className={`${
              sidebarOpen ? "fixed inset-y-0 left-0 z-50 w-72" : "hidden"
            } lg:static lg:block w-72 border-r ${cardBg} p-3 flex flex-col gap-3`}
          >
            <div className="flex items-center justify-between lg:hidden">
              <span className="font-display font-bold">AFH AI</span>
              <button onClick={() => setSidebarOpen(false)}>
                <X className="w-5 h-5" />
              </button>
            </div>
            <button
              onClick={() => {
                setActiveConvId(null);
                setSidebarOpen(false);
              }}
              className="w-full py-2.5 px-3 rounded-xl bg-[#1677FF] text-white font-semibold flex items-center justify-center gap-2"
            >
              <Plus className="w-4 h-4" />
              {isArabic ? "محادثة جديدة" : "New Chat"}
            </button>

            <div className="relative">
              <Search className="w-4 h-4 absolute left-3 top-2.5 opacity-60" />
              <input
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder={isArabic ? "بحث..." : "Search history..."}
                className="w-full pl-9 pr-3 py-2 rounded-xl bg-black/20 border border-[#23355C] text-sm"
              />
            </div>

            <button
              onClick={() => setFavoritesOnly(!favoritesOnly)}
              className={`text-xs px-3 py-1.5 rounded-xl border flex items-center gap-1.5 ${
                favoritesOnly ? "border-[#00D5FF] text-[#00D5FF]" : "border-[#23355C] opacity-80"
              }`}
            >
              <Star className="w-3.5 h-3.5" />
              {isArabic ? "المفضلة" : "Favorites"}
            </button>

            <div className="flex-1 overflow-y-auto space-y-1.5">
              {filteredConversations.map((c) => (
                <div
                  key={c.id}
                  onClick={() => {
                    setActiveConvId(c.id);
                    setSelectedModelId(c.selectedAfhModelId || "afh_flash");
                    setSidebarOpen(false);
                  }}
                  className={`p-2.5 rounded-xl cursor-pointer flex items-center justify-between gap-2 ${
                    activeConvId === c.id ? "bg-[#1677FF]/25 border border-[#00D5FF]/40" : "hover:bg-white/5"
                  }`}
                >
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium truncate">{c.title}</p>
                    <p className="text-xs opacity-60 truncate">{c.lastMessagePreview}</p>
                  </div>
                  <button
                    onClick={async (e) => {
                      e.stopPropagation();
                      if (!user) return;
                      await updateDoc(doc(db, "users", user.uid, "conversations", c.id), {
                        favorite: !c.favorite,
                        updatedAt: serverTimestamp(),
                      });
                    }}
                    className="p-1 opacity-70 hover:opacity-100"
                  >
                    <Star className={`w-3.5 h-3.5 ${c.favorite ? "text-[#00D5FF] fill-[#00D5FF]" : ""}`} />
                  </button>
                  <button
                    onClick={async (e) => {
                      e.stopPropagation();
                      if (!user) return;
                      const convRef = doc(db, "users", user.uid, "conversations", c.id);
                      const msgsSnap = await getDocs(collection(convRef, "messages"));
                      const batch = writeBatch(db);
                      msgsSnap.docs.forEach((mDoc) => batch.delete(mDoc.ref));
                      batch.delete(convRef);
                      await batch.commit();
                      if (activeConvId === c.id) setActiveConvId(null);
                    }}
                    className="p-1 opacity-70 hover:text-red-400"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              ))}
            </div>
          </aside>

          {/* Chat Thread */}
          <main className="flex-1 flex flex-col min-w-0">
            <div className={`border-b ${cardBg} px-4 py-2.5 flex items-center justify-between gap-3`}>
              <select
                value={selectedModelId}
                onChange={(e) => {
                  const next = models.find((m) => m.id === e.target.value);
                  if (next) {
                    setSelectedModelId(next.id);
                    setNotice(`Switched to ${next.afhModelName}. Context preserved.`);
                  }
                }}
                className="rounded-xl bg-[#162340] border border-[#00D5FF]/40 px-3 py-1.5 text-sm font-semibold"
              >
                {models.map((m) => (
                  <option key={m.id} value={m.id} disabled={!m.enabled}>
                    {isArabic ? m.afhModelNameAr : m.afhModelName} {!m.enabled ? "(Disabled)" : ""}
                  </option>
                ))}
              </select>
              <span className="text-xs opacity-75">
                {activeModel?.capabilities.join(" • ")}
              </span>
            </div>

            {notice && (
              <div className="mx-4 mt-2 px-3 py-2 rounded-xl bg-[#00D5FF]/15 text-[#00D5FF] text-xs flex justify-between">
                <span>{notice}</span>
                <button onClick={() => setNotice(null)}>
                  <X className="w-3.5 h-3.5" />
                </button>
              </div>
            )}
            {errorBanner && (
              <div className="mx-4 mt-2 px-3 py-2 rounded-xl bg-red-500/15 text-red-400 text-xs flex justify-between">
                <span>{errorBanner}</span>
                <button onClick={() => setErrorBanner(null)}>
                  <X className="w-3.5 h-3.5" />
                </button>
              </div>
            )}

            <div className="flex-1 overflow-y-auto p-4 space-y-4 max-w-4xl mx-auto w-full">
              {!user && (
                <div className={`rounded-2xl border p-8 text-center space-y-4 ${cardBg}`}>
                  <img src="/afh-logo.jpg" alt="AFH AI" className="w-16 h-16 rounded-2xl mx-auto" />
                  <h2 className="font-display text-xl font-bold">
                    {isArabic ? "سجّل الدخول للمزامنة مع التطبيق" : "Sign in to sync Web & Android"}
                  </h2>
                  <button
                    onClick={handleSignIn}
                    className="px-6 py-2.5 rounded-xl bg-[#1677FF] text-white font-semibold"
                  >
                    {isArabic ? "المتابعة باستخدام Google" : "Continue with Google"}
                  </button>
                </div>
              )}

              {user && messages.length === 0 && !isStreaming && (
                <div className="text-center py-14 space-y-3">
                  <img src="/afh-logo.jpg" alt="AFH AI" className="w-16 h-16 rounded-2xl mx-auto border border-[#00D5FF]/40" />
                  <h2 className="font-display text-2xl font-bold">
                    {isArabic ? "كيف يمكن لـ AFH AI مساعدتك؟" : "How can AFH AI help you today?"}
                  </h2>
                  <p className="text-sm opacity-75">{activeModel?.description}</p>
                </div>
              )}

              {messages.map((m) => (
                <div key={m.id} className={`flex ${m.role === "user" ? "justify-end" : "justify-start"}`}>
                  <div
                    className={`max-w-[85%] rounded-2xl p-4 border ${
                      m.role === "user"
                        ? "bg-[#1677FF]/20 border-[#00D5FF]/40"
                        : `${cardBg}`
                    }`}
                  >
                    <div className="text-xs font-semibold text-[#00D5FF] mb-1">
                      {m.role === "user" ? (isArabic ? "أنت" : "You") : m.afhModelId.replace("afh_", "AFH ").toUpperCase()}
                    </div>
                    <pre className="whitespace-pre-wrap font-sans text-sm leading-relaxed">{m.content}</pre>
                    <div className="flex gap-2 mt-2 opacity-70">
                      <button
                        onClick={() => navigator.clipboard.writeText(m.content)}
                        className="text-xs flex items-center gap-1 hover:text-[#00D5FF]"
                      >
                        <Copy className="w-3 h-3" /> Copy
                      </button>
                      {m.role === "assistant" && (
                        <button
                          onClick={() => {
                            const idx = messages.indexOf(m);
                            const prevUser = messages.slice(0, idx).reverse().find((x) => x.role === "user");
                            if (prevUser) handleSend(prevUser.content);
                          }}
                          className="text-xs flex items-center gap-1 hover:text-[#00D5FF]"
                        >
                          <RefreshCw className="w-3 h-3" /> Regenerate
                        </button>
                      )}
                      {m.role === "user" && (
                        <button
                          onClick={() => setInputText(m.content)}
                          className="text-xs flex items-center gap-1 hover:text-[#00D5FF]"
                        >
                          <Edit3 className="w-3 h-3" /> Edit
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              ))}

              {isStreaming && (
                <div className={`max-w-[85%] rounded-2xl p-4 border ${cardBg}`}>
                  <div className="text-xs font-semibold text-[#00E6C7] mb-1">AFH AI</div>
                  <pre className="whitespace-pre-wrap font-sans text-sm leading-relaxed">
                    {streamingContent || "Thinking..."}
                  </pre>
                </div>
              )}
            </div>

            {/* Composer */}
            <div className={`border-t ${cardBg} p-3`}>
              <div className="max-w-4xl mx-auto space-y-2">
                {attachments.length > 0 && (
                  <div className="flex flex-wrap gap-2">
                    {attachments.map((a, i) => (
                      <span key={i} className="text-xs px-2.5 py-1 rounded-lg bg-[#1677FF]/20 flex items-center gap-1.5">
                        <FileText className="w-3 h-3" /> {a.name}
                        <button onClick={() => setAttachments((p) => p.filter((_, idx) => idx !== i))}>
                          <X className="w-3 h-3" />
                        </button>
                      </span>
                    ))}
                  </div>
                )}
                <div className="flex items-end gap-2">
                  {(activeModel?.capabilities.includes("Vision") || activeModel?.capabilities.includes("Files")) && (
                    <label className="p-3 rounded-2xl border border-[#23355C] cursor-pointer hover:border-[#00D5FF]">
                      <Paperclip className="w-4 h-4 text-[#00D5FF]" />
                      <input type="file" className="hidden" onChange={handleFileSelect} />
                    </label>
                  )}
                  <textarea
                    rows={1}
                    value={inputText}
                    onChange={(e) => setInputText(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" && !e.shiftKey) {
                        e.preventDefault();
                        handleSend();
                      }
                    }}
                    placeholder={isArabic ? "اكتب رسالتك إلى AFH AI..." : "Message AFH AI..."}
                    className="flex-1 rounded-2xl bg-black/20 border border-[#23355C] px-4 py-3 text-sm resize-none focus:outline-none focus:border-[#00D5FF]"
                  />
                  {isStreaming ? (
                    <button
                      onClick={() => abortRef.current?.abort()}
                      className="p-3 rounded-2xl bg-red-500 text-white"
                    >
                      <Square className="w-4 h-4" />
                    </button>
                  ) : (
                    <button
                      onClick={() => handleSend()}
                      className="p-3 rounded-2xl bg-[#1677FF] text-white hover:bg-[#1677FF]/90"
                    >
                      <Send className="w-4 h-4" />
                    </button>
                  )}
                </div>
              </div>
            </div>
          </main>
        </div>
      )}

      {view === "settings" && profile && user && (
        <main className="flex-1 max-w-3xl mx-auto w-full px-4 py-8 space-y-5">
          <h1 className="font-display text-3xl font-bold">{isArabic ? "الإعدادات" : "Settings"}</h1>
          <div className={`rounded-2xl border p-6 space-y-4 ${cardBg}`}>
            <div className="flex items-center justify-between">
              <div>
                <p className="font-bold">{profile.name}</p>
                <p className="text-sm opacity-75">{profile.email}</p>
              </div>
              <span className="px-3 py-1 rounded-full text-xs bg-[#00E6C7]/20 text-[#00E6C7] uppercase">
                {profile.role}
              </span>
            </div>
            <p className="text-sm">
              Daily Quota: {profile.dailyRequestsUsed} / {profile.dailyLimit} • Monthly: {profile.monthlyRequestsUsed} /{" "}
              {profile.monthlyLimit}
            </p>
            <div className="flex flex-wrap gap-3">
              <button
                onClick={async () => {
                  const next = isArabic ? "en" : "ar";
                  await updateDoc(doc(db, "users", user.uid), {
                    preferredLanguage: next,
                    updatedAt: serverTimestamp(),
                  });
                }}
                className="px-4 py-2 rounded-xl border border-[#00D5FF]/40 text-sm"
              >
                {isArabic ? "التبديل إلى الإنجليزية" : "Switch to Arabic (RTL)"}
              </button>
              <button
                onClick={() => signOut(auth)}
                className="px-4 py-2 rounded-xl bg-red-500/20 text-red-400 text-sm flex items-center gap-2"
              >
                <LogOut className="w-4 h-4" /> Sign Out
              </button>
            </div>
          </div>
        </main>
      )}

      {view === "admin" && isAdmin && (
        <main className="flex-1 max-w-5xl mx-auto w-full px-4 py-8 space-y-6">
          <h1 className="font-display text-3xl font-bold">AFH AI Admin Dashboard</h1>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className={`rounded-2xl border p-4 ${cardBg}`}>
              <p className="text-2xl font-bold text-[#00D5FF]">{adminUsers.length}</p>
              <p className="text-xs opacity-70">Total Users</p>
            </div>
            <div className={`rounded-2xl border p-4 ${cardBg}`}>
              <p className="text-2xl font-bold text-[#00E6C7]">
                {adminUsers.filter((u) => u.status === "active").length}
              </p>
              <p className="text-xs opacity-70">Active Users</p>
            </div>
            <div className={`rounded-2xl border p-4 ${cardBg}`}>
              <p className="text-2xl font-bold text-[#1677FF]">{models.filter((m) => m.enabled).length}/7</p>
              <p className="text-xs opacity-70">Enabled Models</p>
            </div>
            <div className={`rounded-2xl border p-4 ${cardBg}`}>
              <p className="text-2xl font-bold text-[#875BFF]">{providers.length}</p>
              <p className="text-xs opacity-70">Configured Providers</p>
            </div>
          </div>

          <div className={`rounded-2xl border p-5 space-y-3 ${cardBg}`}>
            <h2 className="font-display font-bold text-lg">Models & Provider Routing</h2>
            {models.map((m) => {
              const map = adminMappings[m.id];
              return (
                <div key={m.id} className="flex flex-wrap items-center justify-between gap-3 py-2 border-b border-white/10">
                  <div>
                    <p className="font-semibold">{m.afhModelName}</p>
                    <p className="text-xs text-[#00D5FF]">
                      {map ? `${map.providerIdentifier} → ${map.underlyingModelId}` : "google_gemini → gemini-3.8-flash"}
                    </p>
                  </div>
                  <button
                    onClick={async () => {
                      await updateDoc(doc(db, "ai_models", m.id), {
                        enabled: !m.enabled,
                        updatedAt: serverTimestamp(),
                      });
                    }}
                    className={`px-3 py-1 rounded-xl text-xs font-semibold ${
                      m.enabled ? "bg-[#00E6C7]/20 text-[#00E6C7]" : "bg-red-500/20 text-red-400"
                    }`}
                  >
                    {m.enabled ? "Enabled" : "Disabled"}
                  </button>
                </div>
              );
            })}
          </div>

          <div className={`rounded-2xl border p-5 space-y-3 ${cardBg}`}>
            <h2 className="font-display font-bold text-lg">User Management ({adminUsers.length})</h2>
            {adminUsers.map((u) => (
              <div key={u.userId} className="flex flex-wrap items-center justify-between gap-3 py-2 border-b border-white/10">
                <div>
                  <p className="font-semibold text-sm">{u.name} ({u.role})</p>
                  <p className="text-xs opacity-70">
                    {u.email} • Daily: {u.dailyRequestsUsed}/{u.dailyLimit}
                  </p>
                </div>
                {u.role !== "admin" && (
                  <button
                    onClick={async () => {
                      await updateDoc(doc(db, "users", u.userId), {
                        status: u.status === "active" ? "suspended" : "active",
                        updatedAt: serverTimestamp(),
                      });
                    }}
                    className={`px-3 py-1 rounded-xl text-xs font-semibold ${
                      u.status === "active" ? "bg-[#00E6C7]/20 text-[#00E6C7]" : "bg-red-500/20 text-red-400"
                    }`}
                  >
                    {u.status === "active" ? "Active" : "Suspended"}
                  </button>
                )}
              </div>
            ))}
          </div>

          <div className={`rounded-2xl border p-5 space-y-3 ${cardBg}`}>
            <h2 className="font-display font-bold text-lg">Recent Usage & Error Logs</h2>
            {usageLogs.length === 0 ? (
              <p className="text-sm opacity-70">No usage events recorded yet.</p>
            ) : (
              usageLogs.slice(0, 15).map((log) => (
                <div key={log.id} className="flex items-center justify-between text-xs py-1.5 border-b border-white/10">
                  <span>
                    <strong>{log.afhModelId}</strong> • {log.latencyMs} ms {log.errorCode ? `• ${log.errorCode}` : ""}
                  </span>
                  <span className={log.status === "success" ? "text-[#00E6C7]" : "text-red-400"}>
                    {log.status.toUpperCase()}
                  </span>
                </div>
              ))
            )}
          </div>
        </main>
      )}
    </div>
  );
}
