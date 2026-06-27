import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext.jsx";
import { subscribeToUser } from "../services/socket.js";
import { BellRing, X } from "lucide-react";

export default function GlobalListener() {
  const { user } = useAuth();
  const [calledToken, setCalledToken] = useState(null);

  useEffect(() => {
    if (!user || user.role !== "USER") return;

    const unsubscribe = subscribeToUser(user.id, (message) => {
      // The message could be a TokenResponse or QueueStatusResponse
      const tokenData = message.token ? message.token : message;
      
      if (tokenData && tokenData.status === "CALLED") {
        setCalledToken(tokenData);
        playTingSound();
      }
    });

    return () => unsubscribe();
  }, [user]);

  const playTingSound = () => {
    try {
      const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
      const oscillator = audioCtx.createOscillator();
      const gainNode = audioCtx.createGain();
      
      oscillator.type = "sine";
      oscillator.frequency.setValueAtTime(880, audioCtx.currentTime); // A5 note
      oscillator.frequency.exponentialRampToValueAtTime(1760, audioCtx.currentTime + 0.1); // A6 note
      
      gainNode.gain.setValueAtTime(1, audioCtx.currentTime);
      gainNode.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 1.5);
      
      oscillator.connect(gainNode);
      gainNode.connect(audioCtx.destination);
      
      oscillator.start();
      oscillator.stop(audioCtx.currentTime + 1.5);
    } catch (e) {
      console.error("Audio playback failed", e);
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
        <button 
          onClick={() => setCalledToken(null)}
          style={{
            position: "absolute",
            top: "1rem",
            right: "1rem",
            background: "none",
            border: "none",
            cursor: "pointer",
            color: "#6b7280"
          }}
        >
          <X size={24} />
        </button>
        
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
        <p style={{ fontSize: "1rem", color: "#4b5563", marginBottom: "1.5rem" }}>
          Your token <strong>{calledToken.tokenNumber}</strong> has been called at <strong>{calledToken.counterName}</strong>. Please proceed to the counter immediately.
        </p>
        
        <button 
          onClick={() => {
            setCalledToken(null);
            window.location.reload(); // Refresh the page as requested
          }}
          className="primary-action"
          style={{ width: "100%", padding: "0.75rem", fontSize: "1.125rem", justifyContent: "center" }}
        >
          Acknowledge & Refresh
        </button>
      </div>
      
      <style>
        {`
          @keyframes slideIn {
            from { opacity: 0; transform: translateY(20px) scale(0.95); }
            to { opacity: 1; transform: translateY(0) scale(1); }
          }
          @keyframes pulse {
            0% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.4); }
            70% { box-shadow: 0 0 0 20px rgba(16, 185, 129, 0); }
            100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
          }
        `}
      </style>
    </div>
  );
}
