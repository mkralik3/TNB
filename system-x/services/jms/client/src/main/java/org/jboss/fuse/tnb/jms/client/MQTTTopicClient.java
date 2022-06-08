package org.jboss.fuse.tnb.jms.client;

import org.jboss.fuse.tnb.common.utils.HTTPUtils;
import org.jboss.fuse.tnb.jms.listener.MQTTMessageListener;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MQTTTopicClient implements BasicJMSOperations<MqttMessage>, TopicClient {
    protected final IMqttClient client;
    protected final String topicName;
    private final MQTTMessageListener listener = new MQTTMessageListener();

    public MQTTTopicClient(String url, String username, String password, String clientId, String topicName) {
        try {
            this.topicName = topicName;
            client = new MqttClient(url, clientId);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setUserName(username);
            options.setPassword(password.toCharArray());
            if (url.startsWith("ssl://")) {
                options.setSocketFactory(HTTPUtils.trustAllSslClient().sslSocketFactory());
                options.setSSLHostnameVerifier((hostname, session) -> true);
                options.setHttpsHostnameVerificationEnabled(false);
            }

            client.connect(options);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create mqtt client instance", e);
        }
    }

    public void send(String message) {
        try {
            client.publish(topicName, new MqttMessage(message.getBytes()));
        } catch (MqttException e) {
            throw new RuntimeException("Unable to publish mqtt message", e);
        }
    }

    @Override
    public String receive(long timeout) {
        return new String(receiveMessage(timeout).getPayload());
    }

    public MqttMessage receiveMessage(long timeout) {
        return listener.next(timeout);
    }

    @Override
    public void subscribe() {
        try {
            client.subscribe(topicName, listener);
            listener.setSubscribed(true);
        } catch (MqttException e) {
            throw new RuntimeException("Unable to subscribe to mqtt topic", e);
        }
    }
}
