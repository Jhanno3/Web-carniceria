import "../styles/app.css";
import { supabase } from "../lib/supabaseClient";
import { createOrder, getMyProfile, markTransferSent, type OrderResponse } from "../lib/api";

interface CheckoutItem {
  productId: string;
  name: string;
  qty: string;
  quantity: number;
  note: string | null;
  subtotal: number | null;
}

interface CheckoutSnapshot {
  items: CheckoutItem[];
  total: number;
}

const money = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS", maximumFractionDigits: 0 });

const SHIPPING_FEE = 2500;
const FREE_SHIPPING_THRESHOLD = 40000;

function esc(value: string): string {
  return value.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c] ?? c);
}

const emptyState = document.getElementById("empty-state") as HTMLParagraphElement;
const content = document.getElementById("checkout-content") as HTMLDivElement;
const confirmation = document.getElementById("confirmation") as HTMLDivElement;
const orderItems = document.getElementById("order-items") as HTMLDivElement;
const orderTotal = document.getElementById("order-total") as HTMLSpanElement;
const contactData = document.getElementById("contact-data") as HTMLDListElement;
const shippingRow = document.getElementById("shipping-row") as HTMLDivElement;
const shippingAmount = document.getElementById("shipping-amount") as HTMLSpanElement;
const transferDetails = document.getElementById("transfer-details") as HTMLDivElement;
const formError = document.getElementById("form-error") as HTMLParagraphElement;
const confirmBtn = document.getElementById("confirm-btn") as HTMLButtonElement;
const confirmationMessage = document.getElementById("confirmation-message") as HTMLParagraphElement;
const transferPending = document.getElementById("transfer-pending") as HTMLDivElement;
const transferConfirmed = document.getElementById("transfer-confirmed") as HTMLParagraphElement;
const markTransferBtn = document.getElementById("mark-transfer-btn") as HTMLButtonElement;

let profileAddressText: string | null = null;
let currentSnapshot: CheckoutSnapshot | null = null;

function readSnapshot(): CheckoutSnapshot | null {
  try {
    const raw = sessionStorage.getItem("checkoutSnapshot");
    if (!raw) return null;
    const parsed = JSON.parse(raw) as CheckoutSnapshot;
    return parsed.items?.length ? parsed : null;
  } catch {
    return null;
  }
}

function renderSnapshot(snapshot: CheckoutSnapshot) {
  currentSnapshot = snapshot;
  orderItems.innerHTML = snapshot.items
    .map(
      (item) => `
        <div class="flex items-center justify-between">
          <span>
            ${esc(item.name)} <span class="text-muted">· ${esc(item.qty)}</span>
            ${item.note ? `<br /><span class="text-muted text-xs">"${esc(item.note)}"</span>` : ""}
          </span>
          <span>${item.subtotal == null ? "$ X.XXX" : money.format(item.subtotal)}</span>
        </div>`,
    )
    .join("");
  renderTotals();
}

function renderTotals() {
  if (!currentSnapshot) return;
  const isEnvio = selectedValue("delivery") === "envio";
  const shipping = isEnvio && currentSnapshot.total < FREE_SHIPPING_THRESHOLD ? SHIPPING_FEE : 0;

  shippingRow.classList.toggle("hidden", !isEnvio);
  if (isEnvio) {
    shippingAmount.textContent = shipping === 0 ? "Gratis" : money.format(shipping);
  }
  orderTotal.textContent = money.format(currentSnapshot.total + shipping);
}

async function renderContact() {
  const profile = await getMyProfile();
  profileAddressText = profile.addressText;
  const rows: Array<[string, string]> = [
    ["Nombre", [profile.firstName, profile.lastName].filter(Boolean).join(" ") || "Sin completar"],
    ["Teléfono", profile.phone ?? "Sin completar"],
    ["Dirección", profile.addressText ?? "Sin completar"],
  ];
  contactData.innerHTML = rows
    .map(([label, value]) => `<div class="flex justify-between gap-4"><dt class="text-muted">${label}</dt><dd>${esc(value)}</dd></div>`)
    .join("");
}

function syncTransferVisibility() {
  transferDetails.classList.toggle("hidden", selectedValue("payment") !== "transferencia");
}

document.querySelectorAll('input[name="payment"]').forEach((input) => {
  input.addEventListener("change", syncTransferVisibility);
});

document.querySelectorAll('input[name="delivery"]').forEach((input) => {
  input.addEventListener("change", renderTotals);
});

document.addEventListener("click", async (e) => {
  const button = (e.target as HTMLElement).closest("[data-copy-alias]") as HTMLButtonElement | null;
  if (!button) return;
  const alias = button.dataset.copyAlias!;
  try {
    await navigator.clipboard.writeText(alias);
    const original = button.textContent;
    button.textContent = "¡Copiado!";
    setTimeout(() => {
      button.textContent = original;
    }, 1500);
  } catch {
    // Sin permiso de portapapeles: el alias ya está visible en pantalla para copiar a mano.
  }
});

function showError(message: string) {
  formError.textContent = message;
  formError.classList.remove("hidden");
}

function hideError() {
  formError.textContent = "";
  formError.classList.add("hidden");
}

function selectedValue(name: string): string {
  return (document.querySelector(`input[name="${name}"]:checked`) as HTMLInputElement).value;
}

confirmBtn.addEventListener("click", async () => {
  hideError();
  const snapshot = readSnapshot();
  if (!snapshot) return;

  const deliveryType = selectedValue("delivery") as "envio" | "retiro";
  const paymentMethod = selectedValue("payment") as "mercadopago" | "transferencia" | "efectivo";

  if (deliveryType === "envio" && !profileAddressText) {
    showError("Completá tu dirección en Mi perfil antes de elegir envío a domicilio.");
    return;
  }

  confirmBtn.disabled = true;
  confirmBtn.textContent = "Confirmando…";

  try {
    const order = await createOrder({
      paymentMethod,
      deliveryType,
      addressText: deliveryType === "envio" ? profileAddressText : null,
      items: snapshot.items.map((item) => ({
        productId: item.productId,
        quantity: item.quantity,
        itemNotes: item.note,
      })),
    });

    localStorage.removeItem("cart");
    sessionStorage.removeItem("checkoutSnapshot");

    if (order.mercadopagoInitPoint) {
      window.location.href = order.mercadopagoInitPoint;
      return;
    }
    showConfirmation(order);
  } catch (error) {
    showError(error instanceof Error ? error.message : "No pudimos confirmar el pedido.");
    confirmBtn.disabled = false;
    confirmBtn.textContent = "Confirmar pedido";
  }
});

function showConfirmation(order: OrderResponse) {
  content.classList.add("hidden");
  confirmation.classList.remove("hidden");

  if (order.paymentMethod === "efectivo") {
    confirmationMessage.textContent = `Pagás ${money.format(order.totalEstimated)} en efectivo al recibir tu pedido.`;
  } else {
    confirmationMessage.textContent = "Elegiste transferencia bancaria. Transferí y avisanos con el botón de abajo.";
    transferPending.classList.remove("hidden");
    markTransferBtn.dataset.orderId = order.id;
  }
}

markTransferBtn.addEventListener("click", async () => {
  const orderId = markTransferBtn.dataset.orderId;
  if (!orderId) return;
  markTransferBtn.disabled = true;
  markTransferBtn.textContent = "Avisando…";
  try {
    await markTransferSent(orderId);
    transferPending.classList.add("hidden");
    transferConfirmed.classList.remove("hidden");
  } catch (error) {
    markTransferBtn.disabled = false;
    markTransferBtn.textContent = "Ya transferí";
  }
});

async function init() {
  const { data } = await supabase.auth.getSession();
  if (!data.session) {
    window.location.href = "/login.html";
    return;
  }

  const snapshot = readSnapshot();
  if (!snapshot) {
    emptyState.classList.remove("hidden");
    return;
  }

  content.classList.remove("hidden");
  renderSnapshot(snapshot);
  syncTransferVisibility();
  await renderContact();
}

init();
