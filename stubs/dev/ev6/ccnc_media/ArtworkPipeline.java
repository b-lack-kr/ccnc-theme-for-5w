package dev.ev6.ccnc_media; public final class ArtworkPipeline<T> { public interface Sink<T> { void onArtwork(T t); } }
