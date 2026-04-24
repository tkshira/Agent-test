import React, { useEffect, useState } from 'react';
import { getRelatedProducts } from '../services/api';
import { purchaseProduct } from '../services/api';

export default function RelatedProducts({ productId, sessionId, onAskAbout, onToggleCompare, compareList }) {
  const [related, setRelated]   = useState([]);
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    getRelatedProducts(productId)
      .then((data) => { if (!cancelled) setRelated(data); })
      .catch(() => { if (!cancelled) setError('Could not load related products.'); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [productId]);

  if (loading) return <p className="related-loading">Loading related products…</p>;
  if (error)   return <p className="related-error">{error}</p>;
  if (!related.length) return <p className="related-empty">No related products found.</p>;

  return (
    <div className="related-products">
      <h4 className="related-title">Related Products</h4>
      <div className="related-grid">
        {related.map((p) => (
          <RelatedCard
            key={p.id}
            product={p}
            sessionId={sessionId}
            onAskAbout={onAskAbout}
            onToggleCompare={onToggleCompare}
            compareSelected={compareList?.some((c) => c.id === p.id)}
          />
        ))}
      </div>
    </div>
  );
}

function RelatedCard({ product, sessionId, onAskAbout, onToggleCompare, compareSelected }) {
  const [buying, setBuying] = useState(false);
  const [bought, setBought] = useState(false);

  const handleBuy = async () => {
    setBuying(true);
    try {
      await purchaseProduct(product.id, 1, sessionId);
      setBought(true);
    } finally {
      setBuying(false);
    }
  };

  const stars = product.rating
    ? '★'.repeat(Math.round(product.rating)) + '☆'.repeat(5 - Math.round(product.rating))
    : null;

  return (
    <div className={`related-card ${compareSelected ? 'compare-selected' : ''}`}>
      <div className="related-card-category">{product.category}</div>
      <div className="related-card-name">{product.name}</div>
      {stars && <div className="related-card-rating">{stars}</div>}
      <div className="related-card-price">{product.currency} {product.price?.toLocaleString?.() ?? product.price}</div>
      <div className="related-card-actions">
        {onAskAbout && (
          <button className="action-button ask-button"
            onClick={() => onAskAbout(`Tell me more about the ${product.name}.`)}>
            Ask AI
          </button>
        )}
        {onToggleCompare && (
          <button
            className={`action-button compare-button ${compareSelected ? 'compare-active' : ''}`}
            onClick={() => onToggleCompare(product)}>
            {compareSelected ? '✓' : '+ Compare'}
          </button>
        )}
        {bought ? (
          <span className="buy-success" style={{ fontSize: '0.75rem' }}>Logged!</span>
        ) : (
          <button className="buy-button" style={{ padding: '4px 10px', fontSize: '0.78rem' }}
            onClick={handleBuy} disabled={buying || product.availability === 'Out of Stock'}>
            {buying ? '…' : 'Buy'}
          </button>
        )}
      </div>
    </div>
  );
}
