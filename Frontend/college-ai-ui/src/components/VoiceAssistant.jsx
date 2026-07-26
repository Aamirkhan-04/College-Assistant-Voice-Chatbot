import { useEffect, useRef, useState } from "react";
import AIAnimation from "./AIAnimation";
import VoiceStatus from "./VoiceStatus";
import {
  sendMessage,
  speakDeepgram,
} from "../services/ChatService";
import "../styles/voice.css";

function VoiceAssistant() {

  const [status, setStatus] = useState("Click Start to begin");
  const [isActive, setIsActive] = useState(false);

  const recognitionRef = useRef(null);

  const activeRef = useRef(false);   // stale-closure se bachne ke liye
  const processing = useRef(false);
  const isSpeaking = useRef(false);

  // -----------------------------
  // Speak
  // -----------------------------
  const speak = async (text) => {

    try {

      isSpeaking.current = true;

      setStatus("🔊 Speaking...");

      await speakDeepgram(text);

    } catch (e) {

      console.error(e);

    } finally {

      isSpeaking.current = false;
      processing.current = false;

      if (!activeRef.current) {
        return;
      }

      setStatus("🎤 Listening...");

      // Speak khatam hote hi mic ko explicitly restart karo
      setTimeout(() => {
        if (!activeRef.current) return;
        try {
          recognitionRef.current && recognitionRef.current.start();
        } catch (e) {
          console.log("Restart skip:", e.message);
        }
      }, 300);

    }

  };

  // -----------------------------
  // Recognition ek baar setup, start nahi karte yahan
  // -----------------------------
  useEffect(() => {

    const SpeechRecognition =
      window.SpeechRecognition ||
      window.webkitSpeechRecognition;

    if (!SpeechRecognition) {

      setStatus("Speech Recognition Not Supported");

      return;

    }

    const recognition = new SpeechRecognition();

    recognition.lang = "en-US";
    recognition.continuous = false;
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;

    recognitionRef.current = recognition;

    // -----------------------------
    recognition.onstart = () => {

      console.log("🎤 Listening Started...");

      if (!isSpeaking.current) {

        setStatus("🎤 Listening...");

      }

    };

    // -----------------------------
    recognition.onresult = async (event) => {

      if (!activeRef.current) {
        return;
      }

      console.log("✅ Speech Detected");
      console.log(event);

      if (processing.current) {
        return;
      }

      processing.current = true;

      try {
        recognition.stop();
      } catch {}

      const text = event.results[0][0].transcript.trim();

      if (!text) {
        processing.current = false;
        return;
      }

      console.log("User :", text);

      setStatus("🤔 Thinking...");

      try {

        console.time("Total Response");

        const response = await sendMessage(text);

        console.log("Backend Response :", response);

        console.timeEnd("Total Response");

        console.log("AI :", response);

        if (response === "__STOP__") {

          processing.current = false;

          recognition.stop();

          activeRef.current = false;
          setIsActive(false);
          setStatus("Stopped. Click Start to begin.");

          return;
        }

        await speak(response);

      } catch (e) {

        processing.current = false;

        console.error(e);

        await speak("Sorry! Something went wrong.");

      }

    };

    // -----------------------------
    recognition.onerror = (event) => {

      console.log("Speech Error :", event.error);

      if (
        !activeRef.current ||
        event.error === "aborted" ||
        processing.current ||
        isSpeaking.current
      ) {
        return;
      }

      setTimeout(() => {

        if (!activeRef.current) return;

        try {

          recognition.start();

        } catch {}

      }, 500);

    };

    // -----------------------------
    recognition.onend = () => {

      if (
        !activeRef.current ||
        processing.current ||
        isSpeaking.current ||
        document.hidden
      ) {
        return;
      }

      setTimeout(() => {

        if (!activeRef.current) return;

        try {

          recognition.start();

        } catch {}

      }, 300);

    };

    return () => {

      activeRef.current = false;
      processing.current = false;
      isSpeaking.current = false;

      try {
        recognition.stop();
      } catch {}

      window.speechSynthesis.cancel();

    };

  }, []);

  // -----------------------------
  // Start / Stop handlers (button se call hote hain)
  // -----------------------------
  const handleStart = () => {

    if (activeRef.current) return;

    activeRef.current = true;
    setIsActive(true);

    processing.current = false;
    isSpeaking.current = false;

    setStatus("👋 Getting ready...");

    speak(
      "Hello! I am the Voice Assistant of Shri G.P.M. Degree College of Science and Commerce. How can I help you today?"
    );

  };

  const handleStop = () => {

    activeRef.current = false;
    setIsActive(false);

    processing.current = false;
    isSpeaking.current = false;

    window.speechSynthesis.cancel();

    try {
      recognitionRef.current && recognitionRef.current.stop();
    } catch {}

    setStatus("Stopped. Click Start to begin.");

  };

  return (

    <div className="voice-container">

      <AIAnimation status={status} />

      <VoiceStatus status={status} />

      <button
        className={isActive ? "control-btn stop" : "control-btn start"}
        onClick={isActive ? handleStop : handleStart}
      >

        {isActive ? "⏹  Stop" : "🎙️  Start"}

      </button>

    </div>

  );

}

export default VoiceAssistant;
