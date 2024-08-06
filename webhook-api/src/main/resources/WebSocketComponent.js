import React, { useEffect, useState } from 'react';
 
const WebSocketComponent = ({ callId, agentId, profileType }) => {
    const [messages, setMessages] = useState([]);
   // const [ws, setWs] = useState(null);
 
    useEffect(() => {
        // Construct the WebSocket URL
    
        const socketUrl = `ws://localhost:8951/contactSummaryStream/callId/${callId}/agentId/${agentId}/profileType/${profileType}`;
        
        // Create a new WebSocket connection
        const webSocket = new WebSocket(socketUrl);
 
        // Handle incoming messages
        webSocket.onmessage = (event) => {
            setMessages((prevMessages) => [...prevMessages, event.data]);
        };
 
        // Handle WebSocket errors
        webSocket.onerror = (error) => {
            console.error('WebSocket error:', error);
        };
 
        // Handle WebSocket connection open
        webSocket.onopen = () => {
            console.log('WebSocket connection established');
        };
 
        // Handle WebSocket connection close
        webSocket.onclose = () => {
            console.log('WebSocket connection closed');
        };
 
        // Cleanup on component unmount
        return () => {
            webSocket.close();
        };
    }, [callId, agentId, profileType]);
 
    return (
        <div>
            {messages.map((msg, index) => (
                <div key={index}>{msg}</div>
            ))}
        </div>
    );
};
 
export default WebSocketComponent;
