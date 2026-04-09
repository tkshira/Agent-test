import React, { useState } from 'react';
import { purchaseProduct } from '../services/api';

/**
 * Displays a single product card with a Buy button.
 * Calls the MCP log_purchase tool via the backend on purchase.
 */
export default function ProductCard({ product, sessionId }) {
  const [buying, setBuying]   = useState(false);
  const [bought, setBought]   = useState(false);
  const [error, setError]     = useState(null);

  const handleBuy = async () => {
    setBuying(true);
    setError(null);
    try {
      await purchaseProduct(product.id, 1, sessionId);
      setBought(true);
    } catch (err) {
      setError('Purchase failed. Please try again.');
    } finally {
      setBuying(false);
    }
  };

  const stars = product.rating
    ? '★'.repeat(Math.round(product.rating)) + '☆'.repeat(5 - Math.round(product.rating))
    : null;

  return (
    <div className="product-card">
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

        {error && <p className="buy-error">{error}</p>}
      </div>
    </div>
  );
}
