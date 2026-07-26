import "../styles/status.css";

function VoiceStatus({ status }) {

    return (

        <h2 className="voice-status">

            🎤 {status}

        </h2>

    );

}

export default VoiceStatus;