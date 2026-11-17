package dev.ev6.nowplaying; public final class ArtworkPipeline<T> { public interface Sink<T> { void onArtwork(T t); } }
