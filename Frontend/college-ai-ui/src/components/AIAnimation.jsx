import "../styles/voice.css";

function AIAnimation({ status }) {

  let orbClass = "orb idle";

  if (status.includes("Listening")) {
    orbClass = "orb listening";
  }

  if (status.includes("Thinking")) {
    orbClass = "orb thinking";
  }

  if (status.includes("Speaking")) {
    orbClass = "orb speaking";
  }

  return (

    <div className="orb-container">

      <div className={orbClass}></div>

    </div>

  );

}

export default AIAnimation;
