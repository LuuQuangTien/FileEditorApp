import { useState, useRef, useEffect } from "react";
import { Send, Bot, User, Sparkles, Menu } from "lucide-react";
import { Button } from "../components/ui/button";
import { Textarea } from "../components/ui/textarea";

type Message = {
  id: string;
  role: "user" | "assistant";
  content: string;
  timestamp: Date;
};

export function Chat() {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: "1",
      role: "assistant",
      content:
        "Hello! I'm your AI assistant for document management. I can help you with:\n\n• Searching through your documents\n• Summarizing PDFs and long documents\n• Answering questions about your files\n• Organizing and categorizing documents\n• Extracting specific information\n\nHow can I help you today?",
      timestamp: new Date(),
    },
  ]);
  const [input, setInput] = useState("");
  const [isTyping, setIsTyping] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages, isTyping]);

  const handleSend = () => {
    if (!input.trim()) return;

    const userMessage: Message = {
      id: Date.now().toString(),
      role: "user",
      content: input,
      timestamp: new Date(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setInput("");
    setIsTyping(true);

    // Simulate AI response
    setTimeout(() => {
      const responses = [
        "I can help you with that! Based on your documents, here's what I found...",
        "That's a great question. Let me analyze your documents to provide you with the most accurate information.",
        "I've searched through your document library. Here are the relevant findings...",
        "Based on the content of your files, I can provide you with the following insights...",
        "I've processed your request. Here's a summary of what I discovered in your documents...",
      ];

      const aiMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: "assistant",
        content: responses[Math.floor(Math.random() * responses.length)],
        timestamp: new Date(),
      };

      setMessages((prev) => [...prev, aiMessage]);
      setIsTyping(false);
    }, 1500);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const suggestedPrompts = [
    "Summarize my latest documents",
    "Find documents about Q4 reports",
    "What's in my financial PDFs?",
    "Organize my documents by category",
  ];

  return (
    <div className="h-full flex flex-col bg-neutral-50">
      {/* Header */}
      <div className="bg-white border-b border-neutral-200 px-4 py-3">
        <div className="flex items-center justify-between mb-2">
          <div className="flex items-center gap-2">
            <Sparkles className="size-5 text-blue-600" />
            <h1 className="text-xl">AI Assistant</h1>
          </div>
          <button className="p-2 hover:bg-neutral-100 rounded-lg">
            <Menu className="size-5" />
          </button>
        </div>
        <p className="text-sm text-neutral-600">
          Ask questions about your documents
        </p>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-auto px-4 py-4" ref={scrollRef}>
        <div className="space-y-4">
          {messages.map((message) => (
            <div
              key={message.id}
              className={`flex gap-3 ${
                message.role === "user" ? "flex-row-reverse" : ""
              }`}
            >
              <div className="size-10 shrink-0 rounded-full flex items-center justify-center">
                {message.role === "assistant" ? (
                  <div className="size-10 bg-blue-100 rounded-full flex items-center justify-center">
                    <Bot className="size-5 text-blue-600" />
                  </div>
                ) : (
                  <div className="size-10 bg-neutral-200 rounded-full flex items-center justify-center">
                    <User className="size-5 text-neutral-600" />
                  </div>
                )}
              </div>
              <div className="flex-1 min-w-0">
                <div
                  className={`rounded-2xl p-4 ${
                    message.role === "user"
                      ? "bg-blue-600 text-white"
                      : "bg-white border border-neutral-200"
                  }`}
                >
                  <p className="whitespace-pre-wrap text-sm">{message.content}</p>
                </div>
                <p className="text-xs text-neutral-500 mt-1 px-2">
                  {message.timestamp.toLocaleTimeString([], {
                    hour: "2-digit",
                    minute: "2-digit",
                  })}
                </p>
              </div>
            </div>
          ))}

          {isTyping && (
            <div className="flex gap-3">
              <div className="size-10 shrink-0 bg-blue-100 rounded-full flex items-center justify-center">
                <Bot className="size-5 text-blue-600" />
              </div>
              <div className="bg-white border border-neutral-200 rounded-2xl px-4 py-3">
                <div className="flex gap-1">
                  <div className="size-2 bg-neutral-400 rounded-full animate-bounce"></div>
                  <div className="size-2 bg-neutral-400 rounded-full animate-bounce [animation-delay:0.2s]"></div>
                  <div className="size-2 bg-neutral-400 rounded-full animate-bounce [animation-delay:0.4s]"></div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Suggested Prompts */}
      {messages.length === 1 && (
        <div className="px-4 pb-4">
          <p className="text-sm text-neutral-600 mb-2">Suggested:</p>
          <div className="grid grid-cols-1 gap-2">
            {suggestedPrompts.map((prompt, index) => (
              <button
                key={index}
                onClick={() => setInput(prompt)}
                className="text-left p-3 bg-white border border-neutral-200 rounded-xl hover:border-blue-300 hover:bg-blue-50 transition-colors text-sm active:scale-[0.98]"
              >
                {prompt}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Input Area */}
      <div className="p-4 bg-white border-t border-neutral-200">
        <div className="flex gap-2 items-end">
          <Textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ask anything..."
            className="resize-none min-h-[44px] max-h-[120px]"
            rows={1}
          />
          <Button
            onClick={handleSend}
            disabled={!input.trim() || isTyping}
            size="icon"
            className="size-11 shrink-0"
          >
            <Send className="size-5" />
          </Button>
        </div>
      </div>
    </div>
  );
}