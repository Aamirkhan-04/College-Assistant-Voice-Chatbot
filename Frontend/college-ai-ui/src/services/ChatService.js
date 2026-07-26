import axios from "axios";

const CHAT_API = "http://localhost:8080/chat";

const SESSION_ID = crypto.randomUUID();

// --------------------------------------
// Send Message
// --------------------------------------
export const sendMessage = async (message) => {

    try {

        const response = await axios.get(CHAT_API, {

            params: {

                sessionId: SESSION_ID,
                message: message.trim()

            },

            timeout: 30000

        });

        return response.data;

    } catch (error) {

        console.error("Chat API Error :", error);

        return "Sorry! I'm unable to process your request right now.";

    }

};

// --------------------------------------
// Browser's Built-in Text To Speech
// (Free, no API key, no network call — instant)
// --------------------------------------
export const speakDeepgram = (text) => {

    return new Promise((resolve) => {

        if (!("speechSynthesis" in window)) {

            console.error("Speech Synthesis not supported in this browser");
            resolve();
            return;

        }

        // Agar pehle se kuch bol raha ho, usse cancel karo
        window.speechSynthesis.cancel();

        const utterance = new SpeechSynthesisUtterance(text);

        utterance.lang = "en-US";
        utterance.rate = 1;      // speed: 0.5 (slow) to 2 (fast)
        utterance.pitch = 1;     // pitch: 0 to 2

        utterance.onend = () => {
            resolve();
        };

        utterance.onerror = (e) => {
            console.error("Speech Synthesis Error :", e);
            resolve();
        };

        window.speechSynthesis.speak(utterance);

    });

};