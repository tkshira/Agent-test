import React, { useRef } from 'react';

/**
 * Chat input bar with send button and keyboard shortcut (Enter to send).
 */
export default function MessageInput({ value, onChange, onSend, disabled }) {
  const textareaRef = useRef(null);

  const handleKeyDown = (e) => {
    // Send on Enter; allow Shift+Enter for newlines
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      if (!disabled && value.trim()) {
        onSend();
      }
    }
  };

  return (
    <div className="input-bar">
      <textarea
        ref={textareaRef}
        className="message-input"
        placeholder="Ask about our products... (Enter to send)"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={handleKeyDown}
        disabled={disabled}
        rows={1}
      />
      <button
        className="send-button"
        onClick={onSend}
        disabled={disabled || !value.trim()}
        title="Send message"
      >
        {disabled ? (
          <span className="sending-indicator">...</span>
        ) : (
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
            <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
          </svg>
        )}
      </button>
    </div>
  );
}
