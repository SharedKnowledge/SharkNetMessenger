package net.sharksystem.app.messenger;

import net.sharksystem.SharkException;
import net.sharksystem.SortedMessage;
import net.sharksystem.SortedMessageFactory;

import java.util.*;

public class SortedSharkMessageFactory implements SortedMessageFactory {
    private final List<SortedMessage> sortedMessages;
    private int currentDepth;

    public SortedSharkMessageFactory() {
        this.sortedMessages = new ArrayList<>();
        this.currentDepth = 0;
    }

    @Override
    public SortedMessage produceSortedMessage(byte[] messageContent, CharSequence replyToMessageID) {
        Set<SortedMessage> parents = this.getSortedMessagesWithoutChildren();
        this.currentDepth = this.getNextDepth(parents);
        SortedMessage sortedMessage = new SortedMessageImpl(messageContent, this.currentDepth);
        if (parents.size() != 0) {
            for (SortedMessage parent : parents) {
                sortedMessage.setRelation(SortedMessage.CHILD_OF_RELATION, parent.getID());
            }
        }
        if (replyToMessageID != null) {
            sortedMessage.setRelation(SortedMessage.REPLY_TO_RELATION, replyToMessageID);
        }

        this.sortedMessages.add(sortedMessage);
        this.currentDepth = this.sortedMessages.get(this.sortedMessages.size() - 1).getDepth();

        return sortedMessage;
    }

    @Override
    public void addIncomingSortedMessage(SortedMessage sortedMessage) {
        if (sortedMessage.getParents().size() == 0) {
            Set<SortedMessage> parents = this.getSortedMessagesWithoutChildren();
            this.currentDepth = this.getNextDepth(parents);
            sortedMessage.setDepth(this.currentDepth);
            if (parents.size() != 0) {
                for (SortedMessage parent : parents) {
                    sortedMessage.setRelation(SortedMessage.CHILD_OF_RELATION, parent.getID());
                }
            }
        }
        this.sortedMessages.add(sortedMessage);
        // Resort the list
        this.sortSortedMessages();
        this.currentDepth = this.sortedMessages.get(this.sortedMessages.size() - 1).getDepth();
    }

    @Override
    public SortedMessage getSortedMessage(CharSequence messageID) throws SharkException {
        for (SortedMessage message : this.sortedMessages) {
            if (message.getID().equals(messageID)) {
                return message;
            }
        }

        throw new SharkException("No matching message found");
    }

    @Override
    public Set<SortedMessage> getSortedMessagesWithoutChildren() {
        Set<SortedMessage> possibleParents = new HashSet<>();
        for (SortedMessage message : this.sortedMessages) {
            Set<SortedMessage> children = this.getSortedMessageChildren(message.getID());
            if (children.size() == 0) {
                possibleParents.add(message);
            }
        }

        return possibleParents;
    }

    @Override
    public Set<SortedMessage> getSortedMessageChildren(CharSequence messageId) {
        Set<SortedMessage> children = new HashSet<>();
        for (SortedMessage message : this.sortedMessages) {
            if (message.getParents().contains(messageId)) {
                children.add(message);
            }
        }

        return children;
    }

    private int getNextDepth(Set<SortedMessage> parents) {
        if (this.sortedMessages.size() == 0) {
            return 0;
        } else {
            Set<CharSequence> parentIds = new HashSet<>();
            for (SortedMessage parent : parents) {
                parentIds.add(parent.getID());
            }
            this.sortSortedMessages();
            // Sorted message with the highest depth
            SortedMessage lastSortedMessage = this.sortedMessages.get(this.sortedMessages.size() - 1);
            // Check parents of the last and the new sortedMessage
            if (lastSortedMessage.getParents().size() == parentIds.size()
                    && lastSortedMessage.getParents().containsAll(parentIds)) {
                return lastSortedMessage.getDepth();
            }
            return lastSortedMessage.getDepth() + 1;
        }
    }

    private void sortSortedMessages() {
        this.sortedMessages.sort(Comparator.comparingInt(SortedMessage::getDepth));
    }
}
