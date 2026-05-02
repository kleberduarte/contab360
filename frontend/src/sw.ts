/// <reference lib="webworker" />
import { cleanupOutdatedCaches, precacheAndRoute } from "workbox-precaching";

declare const self: ServiceWorkerGlobalScope;

cleanupOutdatedCaches();
// __WB_MANIFEST é injetado pelo vite-plugin-pwa no build
precacheAndRoute(self.__WB_MANIFEST);

self.addEventListener("push", (event) => {
  const fallback = (): NotificationOptions => ({
    body: "Você tem novidades em pendências.",
    icon: "/icons/icon-192.png",
    badge: "/icons/icon-192.png",
    data: { url: "/cliente-pendencias" },
  });

  const show = async () => {
    if (!event.data) {
      await self.registration.showNotification("Contab360", fallback());
      return;
    }
    let data: { title?: string; body?: string; url?: string } = {};
    try {
      data = event.data.json() as typeof data;
    } catch {
      data = { title: "Contab360", body: event.data.text() };
    }
    const title = data.title ?? "Contab360";
    const options: NotificationOptions = {
      body: data.body ?? fallback().body,
      icon: "/icons/icon-192.png",
      badge: "/icons/icon-192.png",
      data: { url: data.url ?? "/cliente-pendencias" },
    };
    await self.registration.showNotification(title, options);
  };

  event.waitUntil(show());
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const url: string = (event.notification.data as { url?: string })?.url ?? "/cliente-pendencias";
  event.waitUntil(
    self.clients
      .matchAll({ type: "window", includeUncontrolled: true })
      .then((clientList) => {
        const existing = clientList.find((c) => c.url.includes(url));
        if (existing) return existing.focus();
        return self.clients.openWindow(url);
      })
  );
});
