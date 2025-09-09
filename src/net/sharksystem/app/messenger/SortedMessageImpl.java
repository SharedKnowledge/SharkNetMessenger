package net.sharksystem.app.messenger;
import net.sharksystem.SharkException;
import net.sharksystem.SortedMessage;

import java.io.*;
import java.security.InvalidParameterException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SortedMessageImpl implements SortedMessage, Serializable {

    private final CharSequence id;
    private final byte[] content;
    private final Set<CharSequence> parents;
    private CharSequence replyTo;
    private int depth;

    public SortedMessageImpl(byte[] content, int depth) {
        this.id = generateMessageID();
        this.content = content;
        this.parents = new HashSet<>();
        this.depth = depth;
    }

    @Override
    public boolean isBefore(SortedMessage sortedMessage) {
        return this.depth <= sortedMessage.getDepth();
    }

    @Override
    public void setRelation(CharSequence relationName, CharSequence otherMessageId) {
        if (relationName == null) {
            throw new InvalidParameterException("The relationName can't be null. Check your input and try again.");
        }
        String relation = relationName.toString();
        switch (relation) {
            case REPLY_TO_RELATION:
                this.replyTo = otherMessageId;
                break;
            case CHILD_OF_RELATION:
                this.parents.add(otherMessageId);
                break;
            default:
                throw new InvalidParameterException("The provided relationName is not correct. Check your input and try again.");
        }
    }

    @Override
    public Set<CharSequence> getRelatedMessages(CharSequence relationName) throws SharkException {
        if (relationName == null) {
            throw new InvalidParameterException("The relationName can't be null. Check your input and try again.");
        }
        Set<CharSequence> relatedMessages = new HashSet<>();
        String relation = relationName.toString();
        switch (relation) {
            case REPLY_TO_RELATION:
                if (this.replyTo == null) {
                    throw new SharkException("No messages matching the provided relation's name found.");
                }
                relatedMessages.add(this.replyTo);
                break;
            case CHILD_OF_RELATION:
                if (this.parents.size() == 0) {
                    throw new SharkException("No messages matching the provided relation's name found.");
                }
                relatedMessages.addAll(this.parents);
                break;
            default:
                throw new InvalidParameterException("The provided relationName is not correct. Check your input and try again.");
        }

        return relatedMessages;
    }

    @Override
    public CharSequence getID() {
        return this.id;
    }

    @Override
    public byte[] getContent() {
        return this.content;
    }

    @Override
    public Set<CharSequence> getParents() {
        return this.parents;
    }

    @Override
    public CharSequence getReplyTo() {
        return this.replyTo;
    }

    @Override
    public int getDepth() {
        return this.depth;
    }

    @Override
    public void setDepth(int depth) {
        this.depth = depth;
    }

    public static byte [] sortedMessageByteArray(SortedMessage sortedMessage) {
        byte[] byteArray = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(sortedMessage);
            out.flush();
            byteArray = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }

        return byteArray;
    }

    public static SortedMessage byteArrayToSortedMessage(byte [] byteArray) {
        SortedMessage sortedMessage = null;
        ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            sortedMessage = (SortedMessage) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }

        return sortedMessage;
    }

    private static CharSequence generateMessageID() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
}
