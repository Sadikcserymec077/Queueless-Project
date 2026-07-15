import { useEffect, useState, useRef } from "react";
import { useAuth } from "../context/AuthContext.jsx";
import { subscribeToUser } from "../services/socket.js";
import { tokensApi } from "../services/api.js";
import { BellRing, X, Clock } from "lucide-react";

// Global audio context to handle browser autoplay policies
let globalAudioCtx = null;

export default function GlobalListener() {
  const { user } = useAuth();
  const [calledToken, setCalledToken] = useState(null);
  const [nextToast, setNextToast] = useState(null);
  const [timeLeft, setTimeLeft] = useState(120);
  const timerRef = useRef(null);

  useEffect(() => {
    // Initialize audio context on first user interaction to bypass autoplay restrictions
    const handleInteraction = () => {
      if (!globalAudioCtx) {
        globalAudioCtx = new (window.AudioContext || window.webkitAudioContext)();
      }
      if (globalAudioCtx.state === 'suspended') {
        globalAudioCtx.resume();
      }
    };
    
    document.addEventListener("click", handleInteraction, { once: true });
    document.addEventListener("touchstart", handleInteraction, { once: true });
    document.addEventListener("keydown", handleInteraction, { once: true });
    
    return () => {
      document.removeEventListener("click", handleInteraction);
      document.removeEventListener("touchstart", handleInteraction);
      document.removeEventListener("keydown", handleInteraction);
    };
  }, []);

  useEffect(() => {
    if (!user) return;

    const unsubscribe = subscribeToUser(user.id, (message) => {
      const tokenData = message.token ? message.token : message;
      const peopleAhead = message.peopleAhead;
      
      if (tokenData && tokenData.status === "CALLED") {
        if (!window.location.pathname.startsWith("/admin")) {
          setCalledToken(tokenData);
          setTimeLeft(120);
          playTingSound();
          speak(`Your token ${tokenData.tokenNumber} is now being served at ${tokenData.counterName}.`);
        }
      } else if (tokenData && tokenData.status === "WAITING" && peopleAhead === 1) {
        if (!window.location.pathname.startsWith("/admin")) {
          setNextToast(tokenData);
          playTingSound();
          setTimeout(() => setNextToast(null), 10000);
        }
      }
    });

    return () => unsubscribe();
  }, [user]);

  useEffect(() => {
    if (calledToken) {
      timerRef.current = setInterval(() => {
        setTimeLeft((prev) => {
          if (prev <= 1) {
            clearInterval(timerRef.current);
            handleAutoSkip(calledToken.id);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    }

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [calledToken]);

  const handleAutoSkip = async (tokenId) => {
    try {
      await tokensApi.skip(tokenId);
      setCalledToken(null);
      window.location.reload();
    } catch (e) {
      console.error("Failed to auto-skip token", e);
      setCalledToken(null);
    }
  };

  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const playTingSound = () => {
    try {
      if (!globalAudioCtx) {
        globalAudioCtx = new (window.AudioContext || window.webkitAudioContext)();
      }
      if (globalAudioCtx.state === 'suspended') {
        globalAudioCtx.resume();
      }
      const oscillator = globalAudioCtx.createOscillator();
      const gainNode = globalAudioCtx.createGain();
      
      oscillator.type = "sine";
      oscillator.frequency.setValueAtTime(880, globalAudioCtx.currentTime); // A5 note
      oscillator.frequency.exponentialRampToValueAtTime(1760, globalAudioCtx.currentTime + 0.1); // A6 note
      
      gainNode.gain.setValueAtTime(1, globalAudioCtx.currentTime);
      gainNode.gain.exponentialRampToValueAtTime(0.01, globalAudioCtx.currentTime + 1.5);
      
      oscillator.connect(gainNode);
      gainNode.connect(globalAudioCtx.destination);
      
      oscillator.start();
      oscillator.stop(globalAudioCtx.currentTime + 1.5);
    } catch (e) {
      console.error("Audio playback failed", e);
    }
  };

  const speak = (text) => {
    if ('speechSynthesis' in window) {
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.rate = 0.9; // Slightly slower for clarity
      utterance.pitch = 1;
      window.speechSynthesis.speak(utterance);
    }
  };

  if (!calledToken) return null;

  return (
    <div style={{
      position: "fixed",
      top: 0,
      left: 0,
      width: "100vw",
      height: "100vh",
      backgroundColor: "rgba(0, 0, 0, 0.6)",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      zIndex: 9999,
      backdropFilter: "blur(4px)"
    }}>
      <div style={{
        backgroundColor: "white",
        padding: "2rem",
        borderRadius: "16px",
        boxShadow: "0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)",
        maxWidth: "400px",
        width: "90%",
        textAlign: "center",
        animation: "slideIn 0.3s ease-out forwards",
        position: "relative"
      }}>
        <div style={{
          background: "#ecfdf5",
          width: "80px",
          height: "80px",
          borderRadius: "50%",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          margin: "0 auto 1.5rem auto",
          color: "#10b981",
          animation: "pulse 2s infinite"
        }}>
          <BellRing size={40} />
        </div>
        
        <h2 style={{ fontSize: "1.5rem", fontWeight: "700", marginBottom: "0.5rem", color: "#111827" }}>
          It's Your Turn!
        </h2>
        <p style={{ fontSize: "1rem", color: "#4b5563", marginBottom: "1rem" }}>
          Your token <strong>{calledToken.tokenNumber}</strong> has been called at <strong>{calledToken.counterName}</strong>. Please proceed to the counter immediately.
        </p>

        <div style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          gap: "0.5rem",
          color: timeLeft < 30 ? "#ef4444" : "#f59e0b",
          fontWeight: "600",
          fontSize: "1.25rem",
          marginBottom: "1.5rem",
          background: timeLeft < 30 ? "#fef2f2" : "#fffbeb",
          padding: "0.5rem 1rem",
          borderRadius: "8px"
        }}>
          <Clock size={20} />
          <span>{formatTime(timeLeft)}</span>
        </div>
        
        <button 
          onClick={() => {
            setCalledToken(null);
          }}
          className="primary-action"
          style={{ width: "100%", padding: "0.75rem", fontSize: "1.125rem", justifyContent: "center" }}
        >
          Acknowledge
        </button>
      </div>
      
      <style>
        {`
          @keyframes slideIn {
            from { opacity: 0; transform: translateY(20px) scale(0.95); }
            to { opacity: 1; transform: translateY(0) scale(1); }
          }
          @keyframes slideUp {
            from { opacity: 0; transform: translateY(100%); }
            to { opacity: 1; transform: translateY(0); }
          }
          @keyframes pulse {
            0% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.4); }
            70% { box-shadow: 0 0 0 20px rgba(16, 185, 129, 0); }
            100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
          }
        `}
      </style>

      {/* Render Toast for "Next in line" */}
      {nextToast && !calledToken && (
        <div style={{
          position: "fixed",
          bottom: "2rem",
          right: "2rem",
          backgroundColor: "#fff",
          borderLeft: "4px solid #3b82f6",
          padding: "1rem 1.5rem",
          borderRadius: "8px",
          boxShadow: "0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)",
          display: "flex",
          alignItems: "center",
          gap: "1rem",
          zIndex: 9999,
          animation: "slideUp 0.3s ease-out forwards",
          maxWidth: "400px"
        }}>
          <div style={{ color: "#3b82f6" }}>
            <BellRing size={24} />
          </div>
          <div style={{ flex: 1 }}>
            <h4 style={{ margin: 0, fontSize: "1rem", color: "#111827", fontWeight: "600" }}>Get Ready!</h4>
            <p style={{ margin: "0.25rem 0 0 0", fontSize: "0.875rem", color: "#4b5563" }}>
              You are next in line for <strong>{nextToast.counterName}</strong>.
            </p>
          </div>
          <button 
            onClick={() => setNextToast(null)}
            style={{ background: "transparent", border: "none", cursor: "pointer", color: "#9ca3af" }}
          >
            <X size={20} />
          </button>
        </div>
      )}
    </div>
  );
}
