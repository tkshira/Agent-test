import axios from 'axios';

const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL || '',
  headers: { 'Content-Type': 'application/json' },
});

/**
 * Send a chat message to the Azure AI Foundry agent.
 * @param {string} message - User input
 * @param {string|null} threadId - Existing thread ID; null to start a new conversation
 * @returns {Promise<{message: string, products: Array, threadId: string}>}
 */
export async function sendChatMessage(message, threadId = null) {
  const { data } = await api.post('/api/chat', { message, threadId });
  return data;
}

/**
 * Register a purchase intent via the MCP log_purchase tool.
 * @param {string} productId
 * @param {number} quantity
 * @param {string|null} sessionId - threadId from the chat session
 * @returns {Promise<Object>} Purchase log entry
 */
export async function purchaseProduct(productId, quantity = 1, sessionId = null) {
  const { data } = await api.post('/api/purchase', { productId, quantity, sessionId });
  return data;
}

/**
 * Clean up a conversation thread on the server.
 * @param {string} threadId
 */
export async function deleteThread(threadId) {
  await api.delete(`/api/chat/${threadId}`);
}

/**
 * Fetch products related to a given product.
 * @param {string} productId
 * @param {number} topK
 * @returns {Promise<Array>}
 */
export async function getRelatedProducts(productId, topK = 4) {
  const { data } = await api.get(`/api/products/related/${productId}`, { params: { topK } });
  return data;
}

/**
 * Fetch full details for a list of products for side-by-side comparison.
 * @param {string[]} productIds
 * @returns {Promise<Array>}
 */
export async function compareProducts(productIds) {
  const { data } = await api.post('/api/products/compare', { productIds });
  return data;
}
