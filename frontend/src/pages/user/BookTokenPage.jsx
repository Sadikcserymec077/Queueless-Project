import { CalendarPlus, Search, X } from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import StatusPill from "../../components/StatusPill.jsx";
import { countersApi, organizationsApi, paymentsApi, tokensApi } from "../../services/api.js";
import { apiError } from "../../utils/format.js";

export default function BookTokenPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const initialOrgId = searchParams.get("orgId") || "";

  const [q, setQ] = useState("");
  const [organizations, setOrganizations] = useState([]);
  const [organizationId, setOrganizationId] = useState(initialOrgId);
  const [counters, setCounters] = useState([]);
  const [error, setError] = useState("");

  // Checkout State
  const [selectedCounter, setSelectedCounter] = useState(null);
  const [availableDates, setAvailableDates] = useState([]);
  const [selectedDate, setSelectedDate] = useState(null);
  const [patientCount, setPatientCount] = useState(1);
  const [isProcessing, setIsProcessing] = useState(false);
  const [booked, setBooked] = useState(null);

  const selectedOrg = organizations.find(o => String(o.id) === organizationId);

  useEffect(() => {
    async function loadOrganizations() {
      try {
        const page = await organizationsApi.search({ q: q || undefined, size: 20 });
        setOrganizations(page.content);
        if (!organizationId && page.content.length) {
          setOrganizationId(String(page.content[0].id));
        }
      } catch (err) {
        setError(apiError(err));
      }
    }
    loadOrganizations();
  }, [q]);

  useEffect(() => {
    if (!organizationId) return;
    async function loadCounters() {
      try {
        setCounters(await countersApi.byOrganization(organizationId));
      } catch (err) {
        setError(apiError(err));
      }
    }
    loadCounters();
  }, [organizationId]);

  const openCheckout = async (counter) => {
    setSelectedCounter(counter);
    setSelectedDate(null);
    setPatientCount(1);
    setBooked(null);
    setError("");

    try {
      const dates = await countersApi.availableDates(counter.id);
      setAvailableDates(dates.map(d => new Date(d)));
    } catch (err) {
      console.warn("Could not load available dates", err);
      setAvailableDates([]);
    }
  };

  const closeCheckout = () => {
    setSelectedCounter(null);
    setBooked(null);
  };

  const handleProceedToPay = async () => {
    if (!selectedCounter) return;
    setError("");
    setIsProcessing(true);
    const formattedDate = selectedDate ?
      `${selectedDate.getFullYear()}-${String(selectedDate.getMonth() + 1).padStart(2, '0')}-${String(selectedDate.getDate()).padStart(2, '0')}`
      : null;
    // Fast-path: If the counter is totally free (no booking fee), skip Razorpay
    // Actually, our requirement is to always show Razorpay for payment.
    // If we have a fee of 0, we can just book directly.
    try {
      const orderResponse = await paymentsApi.createOrder({
        counterId: selectedCounter.id,
        patientCount: patientCount,
        scheduledDate: formattedDate
      });

      if (orderResponse.finalAmount === 0) {
        // Free booking!
        const token = await tokensApi.book({
          counterId: selectedCounter.id,
          scheduledDate: formattedDate
        });
        setBooked(token);
        setIsProcessing(false);
        return;
      }

      // Initialize Razorpay
      const options = {
        key: orderResponse.razorpayKeyId,
        amount: orderResponse.finalAmount * 100, // paise
        currency: "INR",
        name: "Q-Smart",
        description: `Booking for ${selectedCounter.counterName}`,
        order_id: orderResponse.razorpayOrderId,
        handler: async function (response) {
          try {
            // Verify payment on backend
            const tokenResponse = await paymentsApi.verify({
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
              scheduledDate: formattedDate
            });
            setBooked(tokenResponse);
          } catch (err) {
            setError(apiError(err));
            alert("Payment verification failed! Order ID: " + response.razorpay_order_id);
          }
        },
        modal: {
          ondismiss: function () {
            setIsProcessing(false);
          }
        },
        theme: {
          color: "#0f172a"
        }
      };

      const rzp = new window.Razorpay(options);
      rzp.on('payment.failed', function (response) {
        setError(response.error.description);
        setIsProcessing(false);
      });
      rzp.open();
    } catch (err) {
      setError(apiError(err));
      setIsProcessing(false);
    }
  };

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Virtual queue</p>
          <h1>Book token</h1>
        </div>
      </div>

      {error ? <div className="alert alert-warning">{error}</div> : null}

      {/* CHECKOUT & SUCCESS MODAL */}
      {(selectedCounter || booked) && (
        <div style={{ position: 'fixed', top: 0, left: 0, width: '100%', height: '100%', backgroundColor: 'rgba(0,0,0,0.6)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000 }}>
          <div style={{ backgroundColor: 'var(--bg-card, #fff)', padding: '2rem', borderRadius: '12px', maxWidth: '450px', width: '90%', boxShadow: '0 4px 20px rgba(0,0,0,0.2)', position: 'relative' }}>
            {!booked && (
              <button onClick={closeCheckout} style={{ position: 'absolute', top: '1rem', right: '1rem', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted)' }}>
                <X size={24} />
              </button>
            )}

            {booked ? (
              <div style={{ textAlign: 'center' }}>
                <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '1rem' }}>
                  <div style={{ width: '60px', height: '60px', borderRadius: '50%', backgroundColor: '#e6f6ec', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#0f5132' }}>
                    <CalendarPlus size={32} />
                  </div>
                </div>
                <h2 style={{ marginTop: 0, marginBottom: '0.5rem', color: 'var(--text-primary)' }}>Booking Confirmed!</h2>
                <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem' }}>Your Token Number is <strong style={{ color: 'var(--text-primary)', fontSize: '1.2rem' }}>{booked.tokenNumber}</strong>.</p>
                {booked.qrCodeData && (
                  <img src={booked.qrCodeData} alt="QR Code" style={{ width: '200px', height: '200px', marginBottom: '1rem' }} />
                )}
                <button className="primary-action" onClick={() => navigate("/user/track")} style={{ width: '100%', justifyContent: 'center' }}>
                  View QR & Track Queue
                </button>
              </div>
            ) : (
              <div>
                <h2 style={{ marginTop: 0, marginBottom: '1.5rem', color: 'var(--text-primary)' }}>Book for {selectedCounter.counterName}</h2>

                <div style={{ marginBottom: '1.5rem' }}>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 'bold' }}>Schedule Date</label>
                  {availableDates.length > 0 ? (
                    <DatePicker
                      selected={selectedDate}
                      onChange={(date) => setSelectedDate(date)}
                      includeDates={availableDates}
                      placeholderText="Select an available date"
                      className="form-control"
                      dateFormat="MMMM d, yyyy"
                      minDate={new Date()}
                    />
                  ) : (
                    <div style={{ padding: '0.5rem', backgroundColor: 'var(--bg-body)', borderRadius: '4px', fontSize: '0.9rem', color: 'var(--text-muted)' }}>
                      No future dates configured. Booking for Live Queue (Today).
                    </div>
                  )}
                </div>

                <div style={{ marginBottom: '1.5rem' }}>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 'bold' }}>Number of Members</label>
                  <select
                    className="form-control"
                    value={patientCount}
                    onChange={e => setPatientCount(Number(e.target.value))}
                  >
                    <option value="1">1 Member</option>
                    <option value="2">2 Members</option>
                    <option value="3">3 Members</option>
                    <option value="4">4 Members</option>
                    <option value="5">5 Members</option>
                  </select>
                </div>

                <div style={{ backgroundColor: 'var(--bg-body)', padding: '1rem', borderRadius: '8px', marginBottom: '1.5rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                    <span>Booking Fee:</span>
                    <span>₹{selectedCounter.bookingFee} / person</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                    <span>Total Members:</span>
                    <span>x {patientCount}</span>
                  </div>
                  {patientCount > 1 && (
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem', color: '#10b981' }}>
                      <span>Discount Applied:</span>
                      <span>{patientCount >= 3 ? '40%' : '30%'}</span>
                    </div>
                  )}
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '1rem', paddingTop: '1rem', borderTop: '1px solid var(--border)', fontWeight: 'bold', fontSize: '1.1rem' }}>
                    <span>Final Amount:</span>
                    <span>₹{patientCount >= 3 ? (patientCount * selectedCounter.bookingFee * 0.6) : patientCount === 2 ? (patientCount * selectedCounter.bookingFee * 0.7) : (patientCount * selectedCounter.bookingFee)}</span>
                  </div>
                </div>

                <button
                  className="primary-action"
                  onClick={handleProceedToPay}
                  disabled={isProcessing}
                  style={{ width: '100%', justifyContent: 'center' }}
                >
                  {isProcessing ? "Processing..." : "Proceed to Pay"}
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      <div className="toolbar">
        <label className="search-box"><Search size={18} /><input value={q} onChange={(event) => setQ(event.target.value)} placeholder="Search organization" /></label>
        <select value={organizationId} onChange={(event) => setOrganizationId(event.target.value)} aria-label="Organization">
          {organizations.map((organization) => <option key={organization.id} value={organization.id}>{organization.name}</option>)}
        </select>
      </div>

      {selectedOrg && (
        <div style={{ backgroundColor: "var(--bg-card, #fff)", border: "1px solid var(--border)", borderRadius: "8px", padding: "1.5rem", marginBottom: "2rem" }}>
          <h2 style={{ marginTop: 0, marginBottom: "0.5rem" }}>{selectedOrg.name}</h2>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: "1rem", color: "var(--text-muted)", fontSize: "0.9rem" }}>
            <div>
              <strong style={{ color: "var(--text-primary)", display: "block", marginBottom: "0.25rem" }}>Address</strong>
              {selectedOrg.address || "Not provided"}
            </div>
            <div>
              <strong style={{ color: "var(--text-primary)", display: "block", marginBottom: "0.25rem" }}>Working Hours</strong>
              {selectedOrg.workingHours || "Not provided"}
            </div>
            {selectedOrg.holidays && (
              <div>
                <strong style={{ color: "var(--text-primary)", display: "block", marginBottom: "0.25rem" }}>Closed On</strong>
                {selectedOrg.holidays}
              </div>
            )}
            <div>
              <strong style={{ color: "var(--text-primary)", display: "block", marginBottom: "0.25rem" }}>Contact</strong>
              {selectedOrg.contactNumber || "Not provided"}
            </div>
          </div>
        </div>
      )}

      <div className="entity-grid">
        {counters.map((counter) => (
          <article className="entity-card" key={counter.id}>
            <p className="eyebrow">{counter.organizationName}</p>
            <h2>{counter.counterName}</h2>
            <p>{counter.serviceType}</p>
            <dl className="compact-metrics">
              <div><dt>Waiting</dt><dd>{counter.waitingTokens}</dd></div>
              <div><dt>Current</dt><dd>{counter.currentToken || "None"}</dd></div>
            </dl>
            <div className="entity-actions">
              <StatusPill value={counter.status} />
              <button
                className="primary-action"
                disabled={counter.status !== "ACTIVE"}
                type="button"
                onClick={() => openCheckout(counter)}
              >
                <CalendarPlus size={18} /> Book
              </button>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
