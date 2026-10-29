import "../styles/app.css";
import { supabase } from "../lib/supabaseClient";
import { confirmTransfer, getMyProfile, listAdminOrders, type AdminOrderResponse } from "../lib/api";

const money = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS", maximumFractionDigits: 0 });

function esc(value: string): string {
  return value.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c] ?? c);
}

const loading = document.getElementById("loading") as HTMLParagraphElement;
const empty = document.getElementById("empty") as HTMLParagraphElement;
const ordersList = document.getElementById("orders-list") as HTMLDivElement;

const STATUS_LABEL: Record<string, string> = {
  pendiente_pago: "Pendiente de pago",
  pago_en_revision: "Transferencia a revisar",
  confirmado: "Confirmado",
  preparando: "Preparando",
  en_camino: "En camino",
  entregado: "Entregado",
  cancelado: "Cancelado",
};

function render(orders: AdminOrderResponse[]) {
  if (!orders.length) {
    empty.classList.remove("hidden");
    return;
  }

  ordersList.innerHTML = orders
    .map(({ order, clientFirstName, clientLastName, clientPhone }) => {
      const clientName = [clientFirstName, clientLastName].filter(Boolean).join(" ") || "Sin nombre";
      const itemsText = order.items.map((i) => `${esc(i.productName)} (${i.quantity} ${i.unit})`).join(", ");
      const needsReview = order.status === "pago_en_revision";
      return `
        <article class="bg-white border border-line rounded-md p-5 text-sm">
          <div class="flex items-center justify-between mb-2">
            <strong>${esc(clientName)}</strong>
            <span class="font-semibold">${money.format(order.totalEstimated)}</span>
          </div>
          <p class="text-muted mb-1">${esc(clientPhone ?? "Sin teléfono")} · ${order.deliveryType === "envio" ? esc(order.addressText ?? "") : "Retira en el local"}</p>
          <p class="text-muted mb-2">${itemsText}</p>
          <div class="flex items-center justify-between">
            <span class="text-xs font-semibold px-2 py-1 rounded-full bg-surface-2">${STATUS_LABEL[order.status] ?? order.status}</span>
            <span class="text-xs text-muted">${order.paymentMethod} · ${order.paymentStatus}</span>
          </div>
          ${needsReview ? `<button type="button" class="btn-secondary mt-3" data-confirm-transfer="${order.id}">Marcar transferencia verificada</button>` : ""}
        </article>`;
    })
    .join("");
}

document.addEventListener("click", async (e) => {
  const button = (e.target as HTMLElement).closest("[data-confirm-transfer]") as HTMLButtonElement | null;
  if (!button) return;
  button.disabled = true;
  button.textContent = "Confirmando…";
  try {
    await confirmTransfer(button.dataset.confirmTransfer!);
    await load();
  } catch {
    button.disabled = false;
    button.textContent = "Marcar transferencia verificada";
  }
});

async function load() {
  const orders = await listAdminOrders();
  loading.classList.add("hidden");
  render(orders);
}

async function init() {
  const { data } = await supabase.auth.getSession();
  if (!data.session) {
    window.location.href = "/login.html";
    return;
  }

  const profile = await getMyProfile();
  if (profile.role !== "admin") {
    window.location.href = "/";
    return;
  }

  await load();
}

init();
