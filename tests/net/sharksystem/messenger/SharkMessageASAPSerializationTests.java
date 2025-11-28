package net.sharksystem.messenger;

import net.sharksystem.SharkException;
import net.sharksystem.app.messenger.InMemoSharkNetMessage;
import net.sharksystem.app.messenger.SharkNetMessage;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.crypto.InMemoASAPKeyStore;
import net.sharksystem.asap.utils.ASAPSerialization;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static net.sharksystem.messenger.TestConstants.*;

public class SharkMessageASAPSerializationTests {
    private byte[] serializeCSMessage(CharSequence message) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASAPSerialization.writeCharSequenceParameter(message, baos);
        return baos.toByteArray();
    }

    private CharSequence deserializeCSContent(byte[] serializedContent) throws IOException, ASAPException {
        ByteArrayInputStream bais = new ByteArrayInputStream(serializedContent);
        return ASAPSerialization.readCharSequenceParameter(bais);
    }

    @Test
    public void serializationTestPlain() throws SharkException, IOException {
        InMemoASAPKeyStore keyStorageAlice = new InMemoASAPKeyStore(ALICE_ID);
        byte[] serializedSNMessage = InMemoSharkNetMessage.serializeMessage(
                SharkNetMessage.SN_CONTENT_TYPE_ASAP_CHARACTER_SEQUENCE,
                this.serializeCSMessage(MESSAGE), ALICE_ID, BOB_ID);

        InMemoSharkNetMessage sharkNetMessage =
                InMemoSharkNetMessage.parseMessage(serializedSNMessage, new ArrayList<>(), keyStorageAlice);

        Assertions.assertEquals(SharkNetMessage.SN_CONTENT_TYPE_ASAP_CHARACTER_SEQUENCE, sharkNetMessage.getContentType());
        Assertions.assertEquals(MESSAGE, this.deserializeCSContent(sharkNetMessage.getContent()));
        Assertions.assertEquals(ALICE_ID, sharkNetMessage.getSender());
        Assertions.assertEquals(1, sharkNetMessage.getRecipients().size());
        Assertions.assertTrue(sharkNetMessage.getRecipients().contains(BOB_ID));
        Assertions.assertFalse(sharkNetMessage.verified());
        Assertions.assertFalse(sharkNetMessage.encrypted());
    }

    @Test
    public void serializationTestSigned() throws SharkException, IOException {
        InMemoASAPKeyStore keyStorageAlice = new InMemoASAPKeyStore(ALICE_ID);
        KeyPair bobKeyPair = keyStorageAlice.createTestPeer(BOB_ID); // Alice knows Bob

        InMemoASAPKeyStore keyStorageBob = new InMemoASAPKeyStore(BOB_ID, bobKeyPair, System.currentTimeMillis());
        keyStorageBob.addKeyPair(ALICE_ID, keyStorageAlice.getKeyPair()); // Bob knows Alice

        byte[] serializedSNMessage = InMemoSharkNetMessage.serializeMessage(
                SharkNetMessage.SN_CONTENT_TYPE_ASAP_CHARACTER_SEQUENCE,
                this.serializeCSMessage(MESSAGE), ALICE_ID, BOB_ID, true, false, keyStorageAlice);

        InMemoSharkNetMessage sharkNetMessage =
                InMemoSharkNetMessage.parseMessage(serializedSNMessage, new ArrayList<>(), keyStorageBob);

        Assertions.assertEquals(SharkNetMessage.SN_CONTENT_TYPE_ASAP_CHARACTER_SEQUENCE, sharkNetMessage.getContentType());
        Assertions.assertEquals(MESSAGE, this.deserializeCSContent(sharkNetMessage.getContent()));
        Assertions.assertEquals(ALICE_ID, sharkNetMessage.getSender());
        Assertions.assertEquals(1, sharkNetMessage.getRecipients().size());
        Assertions.assertTrue(sharkNetMessage.getRecipients().contains(BOB_ID));
        Assertions.assertTrue(sharkNetMessage.verified());
        Assertions.assertFalse(sharkNetMessage.encrypted());
    }

    @Test
    public void serializationTestSignedNotVerified() throws SharkException, IOException {
        InMemoASAPKeyStore keyStorageAlice = new InMemoASAPKeyStore(ALICE_ID);
        KeyPair bobKeyPair = keyStorageAlice.createTestPeer(BOB_ID); // Alice knows Bob
        keyStorageAlice.generateKeyPair(); // create Alice' key pair to sign message

        InMemoASAPKeyStore keyStorageBob = new InMemoASAPKeyStore(BOB_ID, bobKeyPair, System.currentTimeMillis());
        // Bob does not know Alice

        byte[] serializedSNMessage = InMemoSharkNetMessage.serializeMessage(
                SharkNetMessage.SN_CONTENT_TYPE_ASAP_CHARACTER_SEQUENCE,
                this.serializeCSMessage(MESSAGE), ALICE_ID, BOB_ID, true, false, keyStorageAlice);

        InMemoSharkNetMessage sharkNetMessage =
                InMemoSharkNetMessage.parseMessage(serializedSNMessage, new ArrayList<>(), keyStorageBob);

        Assertions.assertEquals(SharkNetMessage.SN_CONTENT_TYPE_ASAP_CHARACTER_SEQUENCE, sharkNetMessage.getContentType());
        Assertions.assertEquals(MESSAGE, this.deserializeCSContent(sharkNetMessage.getContent()));
        Assertions.assertEquals(ALICE_ID, sharkNetMessage.getSender());
        Assertions.assertEquals(1, sharkNetMessage.getRecipients().size());
        Assertions.assertTrue(sharkNetMessage.getRecipients().contains(BOB_ID));
        Assertions.assertFalse(sharkNetMessage.verified());
        Assertions.assertFalse(sharkNetMessage.encrypted());
    }

    @Test
    public void serializationTestEncrypted() throws SharkException, IOException {
        InMemoASAPKeyStore keyStorageAlice = new InMemoASAPKeyStore(ALICE_ID);
        KeyPair bobKeyPair = keyStorageAlice.createTestPeer(BOB_ID); // Alice knows Bob

        InMemoASAPKeyStore keyStorageBob = new InMemoASAPKeyStore(BOB_ID, bobKeyPair, System.currentTimeMillis());
        keyStorageBob.addKeyPair(ALICE_ID, keyStorageAlice.getKeyPair()); // Bob knows Alice

        byte[] serializedSNMessage = InMemoSharkNetMessage.serializeMessage(
                SharkNetMessage.SN_CONTENT_TYPE_ASAP_CHARACTER_SEQUENCE,
                this.serializeCSMessage(MESSAGE), ALICE_ID, BOB_ID, false, true, keyStorageAlice);

        InMemoSharkNetMessage sharkNetMessage =
                InMemoSharkNetMessage.parseMessage(serializedSNMessage, new ArrayList<>(), keyStorageBob);

        Assertions.assertEquals(SharkNetMessage.SN_CONTENT_TYPE_ASAP_CHARACTER_SEQUENCE, sharkNetMessage.getContentType());
        Assertions.assertEquals(MESSAGE, this.deserializeCSContent(sharkNetMessage.getContent()));
        Assertions.assertEquals(ALICE_ID, sharkNetMessage.getSender());
        Assertions.assertEquals(1, sharkNetMessage.getRecipients().size());
        Assertions.assertTrue(sharkNetMessage.getRecipients().contains(BOB_ID));
        Assertions.assertFalse(sharkNetMessage.verified());
        Assertions.assertTrue(sharkNetMessage.encrypted());
    }

    @Test
    public void serializationTestEncryptedAndSigned() throws SharkException, IOException {
        InMemoASAPKeyStore keyStorageAlice = new InMemoASAPKeyStore(ALICE_ID);
        KeyPair bobKeyPair = keyStorageAlice.createTestPeer(BOB_ID); // Alice knows Bob

        InMemoASAPKeyStore keyStorageBob = new InMemoASAPKeyStore(BOB_ID, bobKeyPair, System.currentTimeMillis());
        keyStorageBob.addKeyPair(ALICE_ID, keyStorageAlice.getKeyPair()); // Bob knows Alice

        byte[] serializedSNMessage = InMemoSharkNetMessage.serializeMessage(
                SharkNetMessage.SN_CONTENT_TYPE_ASAP_CHARACTER_SEQUENCE,
                this.serializeCSMessage(MESSAGE), ALICE_ID, BOB_ID, true, true, keyStorageAlice);

        InMemoSharkNetMessage sharkNetMessage =
                InMemoSharkNetMessage.parseMessage(serializedSNMessage, new ArrayList<>(), keyStorageBob);

        Assertions.assertEquals(SharkNetMessage.SN_CONTENT_TYPE_ASAP_CHARACTER_SEQUENCE, sharkNetMessage.getContentType());
        Assertions.assertEquals(MESSAGE, this.deserializeCSContent(sharkNetMessage.getContent()));
        Assertions.assertEquals(ALICE_ID, sharkNetMessage.getSender());
        Assertions.assertEquals(1, sharkNetMessage.getRecipients().size());
        Assertions.assertTrue(sharkNetMessage.getRecipients().contains(BOB_ID));
        Assertions.assertTrue(sharkNetMessage.verified());
        Assertions.assertTrue(sharkNetMessage.encrypted());
    }

    @Test
    public void serializationTestSignedMultipleRecipients() throws SharkException, IOException {
        InMemoASAPKeyStore keyStorageAlice = new InMemoASAPKeyStore(ALICE_ID);

        InMemoASAPKeyStore keyStorageBob = new InMemoASAPKeyStore(BOB_ID);
        keyStorageBob.addKeyPair(ALICE_ID, keyStorageAlice.getKeyPair()); // Bob knows Alice

        // set recipients
        Set<CharSequence> recipients = new HashSet<>();
        recipients.add(BOB_ID);
        recipients.add(CLARA_ID);

        // create Message
        byte[] asapMessage = InMemoSharkNetMessage.serializeMessage(
                SharkNetMessage.SN_CONTENT_TYPE_ASAP_CHARACTER_SEQUENCE,
                this.serializeCSMessage(MESSAGE), ALICE_ID, recipients, true, false, keyStorageAlice);
        // remember when this message was created
        long now = System.currentTimeMillis();

        // parse
        InMemoSharkNetMessage sharkNetMessage =
                InMemoSharkNetMessage.parseMessage(asapMessage, new ArrayList<>(), keyStorageBob);

        Assertions.assertEquals(SharkNetMessage.SN_CONTENT_TYPE_ASAP_CHARACTER_SEQUENCE, sharkNetMessage.getContentType());
        Assertions.assertEquals(MESSAGE, this.deserializeCSContent(sharkNetMessage.getContent()));
        Assertions.assertEquals(2, sharkNetMessage.getRecipients().size());
        Assertions.assertTrue(sharkNetMessage.getRecipients().contains(BOB_ID));
        Assertions.assertTrue(sharkNetMessage.getRecipients().contains(CLARA_ID));
        Assertions.assertEquals(ALICE_ID, sharkNetMessage.getSender());
        Assertions.assertTrue(sharkNetMessage.verified());
        Assertions.assertFalse(sharkNetMessage.encrypted());

        // check timestamp
        long creationTime = sharkNetMessage.getCreationTime();
        long diff = now - creationTime;
        System.out.println("diff == " + diff);
        // should not be that long
        Assertions.assertTrue(diff < 100);
    }

    @Test
    public void serializationTestSignedNoRecipients() throws SharkException, IOException {
        InMemoASAPKeyStore keyStorageAlice = new InMemoASAPKeyStore(ALICE_ID);
        InMemoASAPKeyStore keyStorageBob = new InMemoASAPKeyStore(BOB_ID);
        keyStorageBob.addKeyPair(ALICE_ID, keyStorageAlice.getKeyPair()); // Bob knows Alice

        // create Message
        byte[] asapMessage = InMemoSharkNetMessage.serializeMessage(
                SharkNetMessage.SN_CONTENT_TYPE_ASAP_CHARACTER_SEQUENCE,
                this.serializeCSMessage(MESSAGE), ALICE_ID, (CharSequence)null, false, false, keyStorageAlice);
        // remember when this message was created
        long now = System.currentTimeMillis();

        // parse
        InMemoSharkNetMessage sharkNetMessage =
                InMemoSharkNetMessage.parseMessage(asapMessage, new ArrayList<>(), keyStorageBob);

        Assertions.assertEquals(SharkNetMessage.SN_CONTENT_TYPE_ASAP_CHARACTER_SEQUENCE, sharkNetMessage.getContentType());
        Assertions.assertEquals(MESSAGE, this.deserializeCSContent(sharkNetMessage.getContent()));
        // expect ANY_SNM_PEER as receiver
        Assertions.assertEquals(1, sharkNetMessage.getRecipients().size());
        Assertions.assertEquals(ALICE_ID, sharkNetMessage.getSender());
        Assertions.assertFalse(sharkNetMessage.verified());
        Assertions.assertFalse(sharkNetMessage.encrypted());

        // check timestamp
        long creationTime = sharkNetMessage.getCreationTime();
        long diff = now - creationTime;
        System.out.println("diff == " + diff);
        // should not be that long
        Assertions.assertTrue(diff < 100);
    }
}
