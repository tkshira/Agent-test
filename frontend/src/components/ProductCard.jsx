import React, { useState } from 'react';
import { purchaseProduct } from '../services/api';
import RelatedProducts from './RelatedProducts';

export default function ProductCard({
  product,
  sessionId,
  onAskAbout,
  compareSelected,
  onToggleCompare,
}) {
  const [buying, setBuying]         = useState(false);
  const [bought, setBought]         = useState(false);
  const [buyError, setBuyError]     = useState(null);
  const [showRelated, setShowRelated] = useState(false);

  const handleBuy = async () => {
    setBuying(true);
    setBuyError(null);
    try {
      await purchaseProduct(product.id, 1, sessionId);
      setBought(true);
    } catch {
      setBuyError('Purchase failed. Please try again.');
    } finally {
      setBuying(false);
    }
  };

  const handleAskAbout = () => {
    if (onAskAbout) {
      onAskAbout(`Tell me more about the ${product.name}. What are its key features and who is it best for?`);
    }
  };

  const stars = product.rating
    ? '★'.repeat(Math.round(product.rating)) + '☆'.repeat(5 - Math.round(product.rating))
    : null;

  return (
    <div className={`product-card ${compareSelected ? 'compare-selected' : ''}`}>
      {product.imageUrl && (
        <img
          className="product-image"
          src={product.imageUrl}
          alt={product.name}
          onError={(e) => { e.target.style.display = 'none'; }}
        />
      )}

      <div className="product-body">
        <div className="product-category">{product.category}</div>
        <h3 className="product-name">{product.name}</h3>

        {product.tagline && (
          <p className="product-tagline">"{product.tagline}"</p>
        )}

        <p className="product-description">{product.description}</p>

        <div className="product-meta">
          {stars && <span className="product-rating" title={`${product.rating}/5`}>{stars}</span>}
          <span className={`product-availability ${product.availability === 'In Stock' ? 'in-stock' : 'limited'}`}>
            {product.availability}
          </span>
        </div>

        <div className="product-footer">
          <span className="product-price">
            {product.currency} {product.price?.toLocaleString?.() ?? product.price}
          </span>

          {bought ? (
            <span className="buy-success">Purchase logged!</span>
          ) : (
            <button
              className="buy-button"
              onClick={handleBuy}
              disabled={buying || product.availability === 'Out of Stock'}
            >
              {buying ? 'Processing...' : 'Buy Now'}
            </button>
          )}
        </div>

        {buyError && <p className="buy-error">{buyError}</p>}

        <div className="product-actions">
          {onAskAbout && (
            <button className="action-button ask-button" onClick={handleAskAbout} title="Ask the AI assistant about this product">
              Ask AI
            </button>
          )}

          {onToggleCompare && (
            <button
              className={`action-button compare-button ${compareSelected ? 'compare-active' : ''}`}
              onClick={() => onToggleCompare(product)}
              title={compareSelected ? 'Remove from comparison' : 'Add to comparison'}
            >
              {compareSelected ? '✓ Comparing' : '+ Compare'}
            </button>
          )}

          <button
            className={`action-button related-button ${showRelated ? 'related-active' : ''}`}
            onClick={() => setShowRelated((v) => !v)}
            title="Find related products"
          >
            {showRelated ? 'Hide Related' : 'Find Related'}
          </button>
        </div>

        {showRelated && (
          <RelatedProducts productId={product.id} sessionId={sessionId} onAskAbout={onAskAbout} onToggleCompare={onToggleCompare} />
        )}
      </div>
    </div>
  );
}
