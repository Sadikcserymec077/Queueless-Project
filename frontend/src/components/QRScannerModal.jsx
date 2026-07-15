import { Html5QrcodeScanner } from "html5-qrcode";
import { X, CheckCircle2 } from "lucide-react";
import { useEffect, useState, useRef } from "react";
import { tokensApi } from "../services/api";

export default function QRScannerModal({ isOpen, onClose, onVerified }) {
  const [error, setError] = useState(null);
  const [verifiedToken, setVerifiedToken] = useState(null);
  const scannerRef = useRef(null);

  useEffect(() => {
    if (!isOpen) {
      if (scannerRef.current) {
        scannerRef.current.clear().catch(console.error);
        scannerRef.current = null;
      }
      setVerifiedToken(null);
      setError(null);
      return;
    }

    const scanner = new Html5QrcodeScanner(
      "qr-reader",
      { fps: 10, qrbox: { width: 250, height: 250 } },
      /* verbose= */ false
    );
    
    scannerRef.current = scanner;

    scanner.render(
      async (decodedText) => {
        // Pausing scanner to prevent multiple requests
        scanner.pause(true);
        try {
          const token = await tokensApi.verifyQr(decodedText);
          setVerifiedToken(token);
          setError(null);
          // Wait briefly, then clear scanner UI and call onVerified
          setTimeout(() => {
            scanner.clear();
            if (onVerified) onVerified(token);
          }, 1500);
        } catch (err) {
          setError(err?.response?.data?.message || "Invalid QR Code");
          scanner.resume();
        }
      },
      (errorMessage) => {
        // Ignore parse errors as they happen constantly when no QR is in view
      }
    );

    return () => {
      scanner.clear().catch(console.error);
    };
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <div className="modal-overlay" style={{
      position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, 
      backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 1000, 
      display: 'flex', alignItems: 'center', justifyContent: 'center'
    }}>
      <div className="modal-content" style={{
        backgroundColor: 'var(--card-bg, #fff)', padding: '2rem', 
        borderRadius: '12px', width: '90%', maxWidth: '500px',
        position: 'relative'
      }}>
        <button 
          onClick={onClose}
          style={{ position: 'absolute', top: '1rem', right: '1rem', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted)' }}
        >
          <X size={24} />
        </button>
        
        <h2 style={{ marginTop: 0, marginBottom: '1.5rem', color: 'var(--text-primary)' }}>Scan Token QR Code</h2>
        
        {verifiedToken ? (
          <div style={{ textAlign: 'center', padding: '1rem 0' }}>
            <CheckCircle2 size={64} color="var(--primary-color)" style={{ marginBottom: '1rem', display: 'inline-block' }} />
            <h3 style={{ margin: '0 0 0.5rem', color: '#15803d', fontSize: '1.5rem' }}>Valid Token!</h3>
            <p style={{ margin: '0 0 1rem', color: 'var(--text-muted)' }}>
              User <strong>{verifiedToken.userName}</strong> (Token: {verifiedToken.tokenNumber})
            </p>
            <div style={{ backgroundColor: '#f0fdf4', border: '1px solid #bbf7d0', padding: '1rem', borderRadius: '8px', textAlign: 'left', marginTop: '1rem' }}>
              <p style={{ fontSize: '1.2rem', fontWeight: 'bold', color: '#166534', margin: '0 0 0.5rem' }}>
                Members Admitted: {verifiedToken.patientCount} People
              </p>
              <p style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#1f2937', margin: '0 0 0.5rem' }}>
                Total Paid: ₹{verifiedToken.totalAmountPaid || 0}
              </p>
              <p style={{ margin: 0, color: '#4b5563', fontSize: '0.9rem' }}>
                Status: <span style={{ color: '#16a34a', fontWeight: 'bold' }}>PAID</span>
              </p>
            </div>
          </div>
        ) : (
          <div id="qr-reader" style={{ width: '100%', overflow: 'hidden', borderRadius: '8px' }}></div>
        )}

        {error && !verifiedToken && (
          <div className="alert alert-danger" style={{ marginTop: '1rem', padding: '0.75rem', borderRadius: '6px', backgroundColor: '#fee2e2', color: '#991b1b' }}>
            {error}
          </div>
        )}
      </div>
    </div>
  );
}
