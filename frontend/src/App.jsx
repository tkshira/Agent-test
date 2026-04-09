import React from 'react';
import ChatWindow from './components/ChatWindow';
import './App.css';

export default function App() {
  return (
    <div className="app">
      <header className="app-header">
        <h1>AI Products Chatbot</h1>
        <p>Powered by Azure AI Foundry &amp; Azure AI Search</p>
      </header>
      <ChatWindow />
    </div>
  );
}
