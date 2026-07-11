package io.nebulaaudio.player;

import io.nebulaaudio.source.AudioTrack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Simple FIFO queue of upcoming tracks for one player. Kept separate from
 * AudioPlayer so queue mutation (add/remove/shuffle/skip) doesn't need to
 * touch playback internals directly.
 */
public class TrackScheduler {
    private final Deque<AudioTrack> queue = new ArrayDeque<>();
    private boolean loopTrack = false;
    private boolean loopQueue = false;

    public void enqueue(AudioTrack track) {
        queue.addLast(track);
    }

    public void enqueueNext(AudioTrack track) {
        queue.addFirst(track);
    }

    public AudioTrack poll() {
        return queue.pollFirst();
    }

    public AudioTrack peek() {
        return queue.peekFirst();
    }

    public void requeue(AudioTrack finishedTrack) {
        if (loopTrack) {
            queue.addFirst(finishedTrack);
        } else if (loopQueue) {
            queue.addLast(finishedTrack);
        }
    }

    public List<AudioTrack> asList() {
        return new ArrayList<>(queue);
    }

    public void clear() {
        queue.clear();
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public boolean isLoopTrack() {
        return loopTrack;
    }

    public void setLoopTrack(boolean loopTrack) {
        this.loopTrack = loopTrack;
    }

    public boolean isLoopQueue() {
        return loopQueue;
    }

    public void setLoopQueue(boolean loopQueue) {
        this.loopQueue = loopQueue;
    }
}
