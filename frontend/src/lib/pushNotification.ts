// Mesma base que api.ts: dev usa proxy (/api → backend); produção usa VITE_API_BASE_URL.
const API_ROOT = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? "";
const API_BASE = `${API_ROOT}/api/push`;

function authHeaders(token: string): HeadersInit {
  return {
    "Content-Type": "application/json",
    Authorization: `Bearer ${token}`,
  };
}

/** Converte ArrayBuffer em Base64url (sem padding) para envio ao backend. */
function bufferToBase64url(buffer: ArrayBuffer): string {
  return btoa(String.fromCharCode(...new Uint8Array(buffer)))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}

async function getVapidPublicKey(): Promise<string | null> {
  const res = await fetch(`${API_BASE}/vapid-public-key`);
  if (!res.ok) return null;
  const data = (await res.json()) as { publicKey: string; enabled: string };
  if (data.enabled !== "true" || !data.publicKey) return null;
  return data.publicKey;
}

function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, "+").replace(/_/g, "/");
  const raw = atob(base64);
  return Uint8Array.from([...raw].map((c) => c.charCodeAt(0)));
}

export async function subscribeToPush(accessToken: string): Promise<boolean> {
  if (!accessToken) return false;
  if (!("serviceWorker" in navigator) || !("PushManager" in window)) return false;

  const permission = await Notification.requestPermission();
  if (permission !== "granted") return false;

  const vapidKey = await getVapidPublicKey();
  if (!vapidKey) return false;

  const registration = await navigator.serviceWorker.ready;
  const subscription = await registration.pushManager.subscribe({
    userVisibleOnly: true,
    applicationServerKey: urlBase64ToUint8Array(vapidKey) as BufferSource,
  });

  const key = subscription.getKey("p256dh");
  const authKey = subscription.getKey("auth");
  if (!key || !authKey) return false;

  const res = await fetch(`${API_BASE}/subscribe`, {
    method: "POST",
    headers: authHeaders(accessToken),
    body: JSON.stringify({
      endpoint: subscription.endpoint,
      p256dh: bufferToBase64url(key),
      auth: bufferToBase64url(authKey),
    }),
  });

  return res.ok;
}

export async function unsubscribeFromPush(accessToken: string): Promise<void> {
  if (!("serviceWorker" in navigator) || !accessToken) return;
  const registration = await navigator.serviceWorker.ready;
  const subscription = await registration.pushManager.getSubscription();
  if (!subscription) return;
  await fetch(`${API_BASE}/subscribe`, {
    method: "DELETE",
    headers: authHeaders(accessToken),
    body: JSON.stringify({ endpoint: subscription.endpoint }),
  });
  await subscription.unsubscribe();
}

export async function getSubscriptionStatus(): Promise<"granted" | "denied" | "default" | "unsupported"> {
  if (!("serviceWorker" in navigator) || !("PushManager" in window)) return "unsupported";
  return Notification.permission;
}
