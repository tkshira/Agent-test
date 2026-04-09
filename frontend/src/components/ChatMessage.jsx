import React from 'react';
import ReactMarkdown from 'react-markdown';
import ProductCard from './ProductCard';

/**
 * Renders a single chat bubble.
 * Assistant messages can contain markdown and a product grid.
 */
export default function ChatMessage({ msg, sessionId }) {
  const isUser      = msg.role === 'user';
  const isError     = msg.error;
  const hasProducts = msg.products && msg.products.length > 0;

  return (
    <div className={`message-wrapper ${isUser ? 'user' : 'assistant'}`}>
      <div className={`message-bubble ${isUser ? 'user-bubble' : 'assistant-bubble'} ${isError ? 'error-bubble' : ''}`}>
        {isUser ? (
          <p>{msg.content}</p>
        ) : (
          <ReactMarkdown>{msg.content}</ReactMarkdown>
        )}
      </div>

      {hasProducts && (
        <div className="product-grid">
          {msg.products.map((product) => (
            <ProductCard
              key={product.id}
              product={product}
              sessionId={sessionId}
            />
          ))}
        </div>
      )}
    </div>
  );
}
