/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.samples.mail;

import io.helidon.common.http.Http;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.mail.Authenticator;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.SubjectTerm;

import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

public class MailService implements Service {

    private final Properties mailProperties;

    MailService(Properties mailProperties) {
        this.mailProperties = new Properties(mailProperties);
    }

    @Override
    public void update(Routing.Rules rules) {
        rules
            .get("/search", this::search)
            .get("/send", this::send);
    }

    private void search(ServerRequest request, ServerResponse response) {
        Session session = getMailSession(mailProperties);
        Optional<String> term = request.queryParams().first("term");
        Store store;
        Folder inbox = null;
        try {
            store = session.getStore("gimap");
            if (!store.isConnected()) {
                store.connect();
            }

            inbox = store.getFolder("INBOX");
            SearchTerm searchTerm = new SubjectTerm(term.orElse("helidon"));
            inbox.open(Folder.READ_ONLY);
            Message[] msgs = inbox.search(searchTerm);
            JsonArrayBuilder ab = Json.createBuilderFactory(Collections.emptyMap()).createArrayBuilder();
            for (Message m : msgs) {
                JsonObjectBuilder ob = Json.createBuilderFactory(Collections.emptyMap()).createObjectBuilder();
                ob.add("from", m.getFrom()[0].toString())
                        .add("subject", m.getSubject());
                ab.add(ob.build());
            }
            JsonObject returnObject = Json.createBuilderFactory(Collections.emptyMap()).createObjectBuilder()
                    .add("message", "OK")
                    .add("count", msgs.length)
                    .add("result", ab)
                    .build();
            response.status(Http.Status.OK_200).send(returnObject);
        } catch (MessagingException e) {
            response.send(e);
        } finally {
            if (inbox != null && inbox.isOpen()) {
                try {
                    inbox.close();
                } catch (MessagingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void send(ServerRequest request, ServerResponse response) {
        String user = mailProperties.getProperty("mail.username");
        Session session = getMailSession(mailProperties);
        try {
            InternetAddress userEmail = new InternetAddress(user);
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(user);
            msg.setSender(userEmail);
            msg.setReplyTo(new InternetAddress[] {userEmail});
            msg.setRecipients(Message.RecipientType.TO, new InternetAddress[] {userEmail});
            msg.setSubject("Greetings from Helidon!");
            msg.setText("Sent by Angus Mail/Helidon.");
            Transport.send(msg);
        } catch (MessagingException ex) {
            throw new RuntimeException(ex);
        }
        JsonObject returnObject = Json.createBuilderFactory(Collections.emptyMap()).createObjectBuilder()
                .add("message", "email has been sent!")
                .build();
        response.status(Http.Status.OK_200).send(returnObject);
    }

    private Session getMailSession(Properties properties) {
        return Session.getDefaultInstance(mailProperties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        mailProperties.getProperty("mail.username"),
                        mailProperties.getProperty("mail.password"));
            }
        });
    }

}
