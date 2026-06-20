import { GoogleGenAI } from "@google/genai";

const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });

export async function getWebSummary(url: string) {
  try {
    const response = await ai.models.generateContent({
      model: "gemini-3-flash-preview",
      contents: `You are an AI assistant for Nature Browser. Summarize the content and key purpose of this website based on the URL: ${url}. Keep it concise and nature-themed in tone if appropriate.`,
      config: {
        systemInstruction: "You are the Nature Browser Assistant. Help users understand the web better.",
      }
    });
    return response.text;
  } catch (error) {
    console.error("Gemini Error:", error);
    return "Could not generate summary at this time.";
  }
}

export async function chatWithPage(url: string, history: { role: string; content: string }[], message: string) {
  try {
    const formattedHistory = history.map(h => ({
      role: h.role === 'user' ? 'user' : 'model',
      parts: [{ text: h.content }]
    }));

    const response = await ai.models.generateContent({
      model: "gemini-3-flash-preview",
      contents: [
        ...formattedHistory,
        { role: 'user', parts: [{ text: `Based on the context of this website (${url}), answer the following question: ${message}` }] }
      ],
      config: {
        systemInstruction: "You are the Nature Browser AI Assistant. You help users interact with the web content they are currently viewing. Be helpful, concise, and professional.",
      }
    });
    return response.text;
  } catch (error) {
    console.error("Gemini Chat Error:", error);
    return "The forest spirits are silent. I could not reach the AI at this time.";
  }
}
