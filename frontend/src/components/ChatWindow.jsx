import React, { useState, useEffect, useRef, useCallback } from 'react';
import ChatMessage from './ChatMessage';
import MessageInput from './MessageInput';
import ComparisonPanel from './ComparisonPanel';
import { sendChatMessage, deleteThread } from '../services/api';

const WELCOME_MESSAGE = {
  role: 'assistant',
  content: `Hi! I'm your AI product assistant powered by **Azure AI Foundry**.\n\nAsk me anything about our products — I can help you:\n- **Get detailed info** about any product\n- **Compare products** side by side\n- **Find related products** similar to what you're looking at\n\nWhen you're ready to buy, just click **Buy Now** on any product card!`,
  products: [],
};

export default function ChatWindow() {
  const [messages, setMessages]       = useState([WELCOME_MESSAGE]);
  const [input, setInput]             = useState('');
  const [loading, setLoading]         = useState(false);
  const [threadId, setThreadId]       = useState(null);
  const [compareList, setCompareList] = useState([]);
  const bottomRef                     = useRef(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => {
    return () => {
      if (threadId) deleteThread(threadId).catch(() => {});
    };
  }, [threadId]);

  const handleSend = useCallback(async (textOverride) => {
    const text = (textOverride ?? input).trim();
    if (!text || loading) return;

    const userMessage = { role: 'user', content: text };
    setMessages((prev) => [...prev, userMessage]);
    if (!textOverride) setInput('');
    setLoading(true);

    try {
      const response = await sendChatMessage(text, threadId);

      if (response.threadId && !threadId) {
        setThreadId(response.threadId);
      }

      setMessages((prev) => [
        ...prev,
        {
          role:     'assistant',
          content:  response.message,
          products: response.products || [],
          error:    response.error,
        },
      ]);
    } catch {
      setMessages((prev) => [
        ...prev,
        {
          role:     'assistant',
          content:  'Sorry, I encountered an error. Please try again.',
          error:    true,
          products: [],
        },
      ]);
    } finally {
      setLoading(false);
    }
  }, [input, loading, threadId]);

  const handleAskAbout = useCallback((message) => {
    handleSend(message);
  }, [handleSend]);

  const handleToggleCompare = useCallback((product) => {
    setCompareList((prev) => {
      const exists = prev.some((p) => p.id === product.id);
      if (exists) return prev.filter((p) => p.id !== product.id);
      if (prev.length >= 4) return prev;
      return [...prev, product];
    });
  }, []);

  const handleReset = () => {
    if (threadId) deleteThread(threadId).catch(() => {});
    setThreadId(null);
    setMessages([WELCOME_MESSAGE]);
    setInput('');
    setCompareList([]);
  };

  return (
    <div className="chat-window">
      <div className="chat-header">
        <div className="chat-header-info">
          <div className="agent-avatar">AI</div>
          <div>
            <h2>Product Assistant</h2>
            <span className="agent-subtitle">Powered by Azure AI Foundry</span>
          </div>
        </div>
        <button className="reset-button" onClick={handleReset} title="Start new conversation">
          New Chat
        </button>
      </div>

      <div className="messages-container">
        {messages.map((msg, idx) => (
          <ChatMessage
            key={idx}
            msg={msg}
            sessionId={threadId}
            onAskAbout={handleAskAbout}
            compareList={compareList}
            onToggleCompare={handleToggleCompare}
          />
        ))}

        {loading && (
          <div className="message-wrapper assistant">
            <div className="message-bubble assistant-bubble typing-indicator">
              <span /><span /><span />
            </div>
          </div>
        )}

        <div ref={bottomRef} />
      </div>

      {compareList.length >= 2 && (
        <ComparisonPanel
          products={compareList}
          onClose={() => setCompareList([])}
          onAskCompare={handleAskAbout}
        />
      )}

      <MessageInput
        value={input}
        onChange={setInput}
        onSend={() => handleSend()}
        disabled={loading}
      />
    </div>
  );
}
