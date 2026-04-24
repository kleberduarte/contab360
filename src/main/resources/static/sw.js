/* Contab360 — cache leve para shell estático; API sempre em rede */
const CACHE_NAME = "contab360-pwa-v2";

self.addEventListener("install", (event) => {
    event.waitUntil(
        caches
            .open(CACHE_NAME)
            .then((cache) =>
                cache.addAll([
                    "/manifest.json",
                    "/icons/icon-192.png",
                    "/icons/icon-512.png",
                    "/icons/icon.svg",
                ])
            )
            .then(() => self.skipWaiting())
    );
});

self.addEventListener("activate", (event) => {
    event.waitUntil(
        caches
            .keys()
            .then((keys) => Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k))))
            .then(() => self.clients.claim())
    );
});

self.addEventListener("fetch", (event) => {
    if (event.request.method !== "GET") return;
    const url = new URL(event.request.url);
    if (url.origin !== self.location.origin) return;
    if (url.pathname.startsWith("/api/")) return;

    event.respondWith(
        fetch(event.request)
            .then((res) => {
                if (!res || res.status !== 200 || res.type === "opaque") return res;
                const copy = res.clone();
                caches.open(CACHE_NAME).then((cache) => {
                    cache.put(event.request, copy).catch(() => {});
                });
                return res;
            })
            .catch(() => caches.match(event.request))
    );
});
