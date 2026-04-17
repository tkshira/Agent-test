import React from 'react';

const ROWS = [
  { label: 'Category',     key: 'category' },
  { label: 'Price',        key: 'price',        render: (p) => `${p.currency} ${p.price?.toLocaleString?.() ?? p.price}` },
  { label: 'Rating',       key: 'rating',       render: (p) => p.rating ? `${'★'.repeat(Math.round(p.rating))}${'☆'.repeat(5 - Math.round(p.rating))} (${p.rating})` : '—' },
  { label: 'Availability', key: 'availability' },
  { label: 'Description',  key: 'description' },
];

export default function ComparisonPanel({ products, onClose, onAskCompare }) {
  if (!products || products.length < 2) return null;

  const handleAskCompare = () => {
    if (onAskCompare) {
      const names = products.map((p) => p.name).join(' and ');
      onAskCompare(`Compare ${names}. Which one should I buy and why?`);
    }
  };

  return (
    <div className="comparison-panel">
      <div className="comparison-header">
        <h3 className="comparison-title">Comparing {products.length} Products</h3>
        <div className="comparison-header-actions">
          {onAskCompare && (
            <button className="ask-compare-button" onClick={handleAskCompare}>
              Ask AI to Compare
            </button>
          )}
          <button className="comparison-close" onClick={onClose} title="Close comparison">✕</button>
        </div>
      </div>

      <div className="comparison-table-wrapper">
        <table className="comparison-table">
          <thead>
            <tr>
              <th className="comparison-row-label"></th>
              {products.map((p) => (
                <th key={p.id} className="comparison-product-name">{p.name}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {ROWS.map(({ label, key, render }) => (
              <tr key={key}>
                <td className="comparison-row-label">{label}</td>
                {products.map((p) => (
                  <td key={p.id} className="comparison-cell">
                    {render ? render(p) : (p[key] ?? '—')}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
