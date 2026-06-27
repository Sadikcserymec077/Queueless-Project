import { useState, useEffect, useRef } from "react";
import { MessageCircle, X, Send, Bot, User, Loader2 } from "lucide-react";
import { GoogleGenerativeAI } from "@google/generative-ai";

// Initialize Gemini API
const genAI = new GoogleGenerativeAI(import.meta.env.VITE_GEMINI_API_KEY);

export default function AIChatbot() {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState([
    { role: "assistant", text: "Hi! I'm the QueueLess AI Assistant. How can I help you today?" }
  ]);
  const [input, setInput] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef(null);

  // Auto-scroll to bottom of chat
  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [messages, isOpen]);

  const sendMessage = async (e) => {
    e.preventDefault();
    if (!input.trim() || isLoading) return;

    const userMessage = input.trim();
    setInput("");
    setMessages(prev => [...prev, { role: "user", text: userMessage }]);
    setIsLoading(true);

    try {
      // Use Gemini 1.5 Flash (the recommended model for chat)
      const model = genAI.getGenerativeModel({ 
        model: "gemini-1.5-flash",
        systemInstruction: "You are the QueueLess AI Assistant. You help users navigate the QueueLess platform, which allows them to book virtual tokens for hospitals, banks, and government offices to skip physical lines. Be helpful, concise, and polite."
      });

      // Prepare chat history format for Gemini
      const history = messages.slice(1).map(msg => ({
        role: msg.role === "assistant" ? "model" : "user",
        parts: [{ text: msg.text }]
      }));

      const chat = model.startChat({ history });
      const result = await chat.sendMessage(userMessage);
      const response = await result.response;
      const text = response.text();

      setMessages(prev => [...prev, { role: "assistant", text }]);
      setIsLoading(false);
    } catch (error) {
      console.error("Gemini API Error:", error);
      
      // Fallback Simulated AI (so the UI still works even if the API Key is invalid)
      setTimeout(() => {
        const lower = userMessage.toLowerCase();
        let fallbackText = "I'm the QueueLess AI Assistant! I can help you book tokens, check your queue status, or cancel appointments.";
        
        if (lower.includes("book") || lower.includes("new") || lower.includes("create")) {
          fallbackText = "To book a new token, navigate to the Home page, select an Organization and Counter, and click 'Book Token'. You can book up to 10 tokens per day!";
        } else if (lower.includes("cancel") || lower.includes("delete")) {
          fallbackText = "You can cancel your token from the 'Queue Tracking' page. Just click the 'Request Cancel' button.";
        } else if (lower.includes("late") || lower.includes("delay")) {
          fallbackText = "If you're running late, go to the 'Queue Tracking' page and click 'Come Late'. This will ask the receptionist to move you to the end of the line.";
        } else if (lower.includes("status") || lower.includes("where") || lower.includes("turn") || lower.includes("time")) {
          fallbackText = "You can view your live queue position, estimated wait time, and expected turn time on the 'Queue Tracking' page.";
        } else if (lower.includes("hi") || lower.includes("hello") || lower.includes("hey")) {
          fallbackText = "Hello there! How can I assist you with the QueueLess platform today?";
        } else {
          fallbackText = "I understand. As the QueueLess AI, I recommend checking the Queue Tracking page for your live status, or the Home page to book a new token.";
        }
        
        setMessages(prev => [...prev, { role: "assistant", text: fallbackText }]);
        setIsLoading(false);
      }, 1000);
    }
  };

  return (
    <>
      {/* Chat Toggle Button */}
      <button 
        onClick={() => setIsOpen(!isOpen)}
        style={{
          position: "fixed",
          bottom: "1.5rem",
          right: "1.5rem",
          width: "56px",
          height: "56px",
          borderRadius: "50%",
          background: "var(--primary)",
          color: "white",
          border: "none",
          boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
          display: "grid",
          placeItems: "center",
          cursor: "pointer",
          zIndex: 9999,
          transition: "transform 0.2s"
        }}
        onMouseOver={e => e.currentTarget.style.transform = "scale(1.05)"}
        onMouseOut={e => e.currentTarget.style.transform = "scale(1)"}
      >
        {isOpen ? <X size={24} /> : <MessageCircle size={24} />}
      </button>

      {/* Chat Window */}
      {isOpen && (
        <div style={{
          position: "fixed",
          bottom: "5.5rem",
          right: "1.5rem",
          width: "min(360px, calc(100vw - 3rem))",
          height: "500px",
          backgroundColor: "var(--surface)",
          border: "1px solid var(--line)",
          borderRadius: "12px",
          boxShadow: "0 8px 24px rgba(0,0,0,0.12)",
          display: "flex",
          flexDirection: "column",
          zIndex: 9998,
          overflow: "hidden"
        }}>
          {/* Header */}
          <div style={{
            padding: "1rem",
            background: "var(--primary)",
            color: "white",
            display: "flex",
            alignItems: "center",
            gap: "0.5rem",
            fontWeight: "600"
          }}>
            <Bot size={20} />
            QueueLess AI Assistant
          </div>

          {/* Messages Area */}
          <div style={{
            flex: 1,
            overflowY: "auto",
            padding: "1rem",
            display: "flex",
            flexDirection: "column",
            gap: "1rem",
            background: "var(--surface-soft)"
          }}>
            {messages.map((msg, idx) => (
              <div key={idx} style={{
                display: "flex",
                gap: "0.5rem",
                alignSelf: msg.role === "user" ? "flex-end" : "flex-start",
                maxWidth: "85%"
              }}>
                {msg.role === "assistant" && (
                  <div style={{ width: 28, height: 28, borderRadius: "50%", background: "var(--primary)", color: "white", display: "grid", placeItems: "center", flexShrink: 0 }}>
                    <Bot size={16} />
                  </div>
                )}
                
                <div style={{
                  padding: "0.6rem 0.8rem",
                  borderRadius: "12px",
                  background: msg.role === "user" ? "var(--primary)" : "var(--surface)",
                  color: msg.role === "user" ? "white" : "var(--ink)",
                  border: msg.role === "assistant" ? "1px solid var(--line)" : "none",
                  fontSize: "0.9rem",
                  lineHeight: 1.4
                }}>
                  {msg.text}
                </div>
              </div>
            ))}
            
            {isLoading && (
              <div style={{ display: "flex", gap: "0.5rem", alignSelf: "flex-start" }}>
                <div style={{ width: 28, height: 28, borderRadius: "50%", background: "var(--primary)", color: "white", display: "grid", placeItems: "center", flexShrink: 0 }}>
                  <Loader2 size={16} className="spin" style={{ animation: "spin 1s linear infinite" }} />
                </div>
                <div style={{ padding: "0.6rem 0.8rem", borderRadius: "12px", background: "var(--surface)", border: "1px solid var(--line)", fontSize: "0.9rem" }}>
                  Thinking...
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Input Area */}
          <form onSubmit={sendMessage} style={{
            padding: "0.75rem",
            borderTop: "1px solid var(--line)",
            background: "var(--surface)",
            display: "flex",
            gap: "0.5rem"
          }}>
            <input
              type="text"
              value={input}
              onChange={e => setInput(e.target.value)}
              placeholder="Ask me anything..."
              style={{
                flex: 1,
                minHeight: "36px",
                borderRadius: "20px",
                padding: "0 1rem",
                border: "1px solid var(--line)",
                outline: "none",
                fontSize: "0.9rem"
              }}
            />
            <button 
              type="submit" 
              disabled={!input.trim() || isLoading}
              style={{
                width: "36px",
                height: "36px",
                borderRadius: "50%",
                background: input.trim() && !isLoading ? "var(--primary)" : "var(--line)",
                color: "white",
                border: "none",
                display: "grid",
                placeItems: "center",
                cursor: input.trim() && !isLoading ? "pointer" : "default",
                transition: "background 0.2s"
              }}
            >
              <Send size={16} style={{ marginLeft: "-2px" }} />
            </button>
          </form>
        </div>
      )}
      
      <style>{`
        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
      `}</style>
    </>
  );
}
