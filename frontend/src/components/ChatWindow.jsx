import React, { useState, useEffect, useRef } from 'react';
import ChatMessage from './ChatMessage';
import MessageInput from './MessageInput';
import { sendChatMessage, deleteThread } from '../services/api';

const WELCOME_MESSAGE = {
  role: 'assistant',
  content: `Hi! I'm your AI product assistant powered by **Azure AI Foundry**.\n\nAsk me anything about our products — I can help you find the right item, compare options, and answer questions about features and pricing. When you're ready to buy, just click **Buy Now** on any product card!`,
  products: [],
};

/**
 * Main chat window managing conversation state, thread lifecycle, and
 * auto-scrolling to the latest message.
 */
export default function ChatWindow() {
  const [messages, setMessages]   = useState([WELCOME_MESSAGE]);
  const [input, setInput]         = useState('');
  const [loading, setLoading]     = useState(false);
  const [threadId, setThreadId]   = useState(null);
  const bottomRef                 = useRef(null);

  // Auto-scroll to bottom whenever messages update
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Clean up thread on unmount
  useEffect(() => {
    return () => {
      if (threadId) deleteThread(threadId).catch(() => {});
    };
  }, [threadId]);

  const handleSend = async () => {
    const text = input.trim();
    if (!text || loading) return;

    const userMessage = { role: 'user', content: text };
    setMessages((prev) => [...prev, userMessage]);
    setInput('');
    setLoading(true);

    try {
      const response = await sendChatMessage(text, threadId);

      // Persist thread ID for conversation continuity
      if (response.threadId && !threadId) {
        setThreadId(response.threadId);
      }

      const assistantMessage = {
        role:     'assistant',
        content:  response.message,
        products: response.products || [],
        error:    response.error,
      };

      setMessages((prev) => [...prev, assistantMessage]);
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        {
          role:    'assistant',
          content: 'Sorry, I encountered an error. Please try again.',
          error:   true,
          products: [],
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleReset = () => {
    if (threadId) deleteThread(threadId).catch(() => {});
    setThreadId(null);
    setMessages([WELCOME_MESSAGE]);
    setInput('');
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
          <ChatMessage key={idx} msg={msg} sessionId={threadId} />
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

      <MessageInput
        value={input}
        onChange={setInput}
        onSend={handleSend}
        disabled={loading}
      />
    </div>
  );
}
