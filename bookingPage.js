import {
  fetchSlots,
  fetchCenters,
  populateVaccines,
  checkSessionStatus,
  updateBookingsCountDisplay,
} from "./fetch.js";

function getRoutingUtils() {
  if (!window.routingUtils) {
    console.error(
      "Routing utilities not available yet. Make sure main.js is loaded first."
    );
    return {
      CONTEXTPATH: "/docker-swe3-2024-14-java/",
      getRouteFromPath: (path) =>
        path.replace("/docker-swe3-2024-14-java/", "").replace(/^\//, "") ||
        "hero",
      getFullPath: (route) =>
        "/docker-swe3-2024-14-java/" +
        (route.startsWith("/") ? route.substring(1) : route),
      getApiUrl: (endpoint) =>
        "/docker-swe3-2024-14-java/api/" +
        (endpoint.startsWith("api/") ? endpoint.substring(4) : endpoint),
    };
  }
  return window.routingUtils;
}

export function bookingPage() {
  const bookingForSelect = document.getElementById("bookingFor");
  const otherBookingOptions = document.getElementById("otherBookingOptions");

  bookingForSelect.addEventListener("change", () => {
    if (bookingForSelect.value === "other") {
      otherBookingOptions.style.display = "block";
    } else {
      otherBookingOptions.style.display = "none";
    }
  });
}

export function populateSlotSelect(slots) {
  const slotSelect = document.getElementById("slotSelect");
  slotSelect.innerHTML = "<option value=''>Bitte wählen</option>";

  if (slots.length === 0) {
    const option = document.createElement("option");
    option.disabled = true;
    option.textContent = "Keine freien Termine verfügbar";
    slotSelect.appendChild(option);
    return;
  }

  slots.forEach((slot) => {
    const option = document.createElement("option");
    option.value = slot.id;

    const startTime = new Date(slot.startTime).toLocaleTimeString("de-DE", {
      hour: "2-digit",
      minute: "2-digit",
    });
    const endTime = new Date(slot.endTime).toLocaleTimeString("de-DE", {
      hour: "2-digit",
      minute: "2-digit",
    });

    const availabilityPercent =
      ((slot.capacity - slot.bookedCount) / slot.capacity) * 100;
    const remainingSlots = slot.capacity - slot.bookedCount;

    let availabilityClass = "";
    let availabilityText = "";

    if (remainingSlots === 0) {
      availabilityClass = "slot-full";
      availabilityText = "Ausgebucht";
      option.disabled = true;
    } else if (availabilityPercent <= 25) {
      availabilityClass = "slot-almost-full";
      availabilityText = `Nur noch ${remainingSlots} frei`;
    } else if (availabilityPercent <= 50) {
      availabilityClass = "slot-filling";
      availabilityText = `${remainingSlots} von ${slot.capacity} frei`;
    } else {
      availabilityClass = "slot-available";
      availabilityText = `${remainingSlots} von ${slot.capacity} frei`;
    }

    option.textContent = `${startTime} - ${endTime} (${availabilityText})`;
    option.classList.add(availabilityClass);
    slotSelect.appendChild(option);
  });

  if (!document.getElementById("slot-availability-styles")) {
    const style = document.createElement("style");
    style.id = "slot-availability-styles";
    style.innerHTML = `
      .slot-full { color: #d32f2f; }
      .slot-almost-full { color: #f57c00; font-weight: bold; }
      .slot-filling { color: #388e3c; }
      .slot-available { color: #1976d2; }
    `;
    document.head.appendChild(style);
  }
}

export async function populateCenters() {
  const centers = await fetchCenters();
  const centerSelect = document.getElementById("centerSelect");
  centerSelect.innerHTML = "<option value=''>Bitte wählen</option>";
  centers.forEach((center) => {
    const option = document.createElement("option");
    option.value = center.id;
    option.textContent = center.name;
    centerSelect.appendChild(option);
  });
}

export async function updateSlotsForSelectedDate() {
  const centerSelect = document.getElementById("centerSelect");
  const dateInput = document.getElementById("appointmentDate");
  const vaccineSelect = document.getElementById("vaccineSelect");
  const slotSelect = document.getElementById("slotSelect");

  if (!centerSelect.value) {
    return;
  }

  const centerId = centerSelect.value;
  const date = dateInput.value;

  slotSelect.innerHTML = "<option value=''>Bitte wählen</option>";

  if (centerId && vaccineSelect.children.length <= 1) {
    await populateVaccines(centerId);
  }

  if (centerId && date) {
    try {
      const slots = await fetchSlots(centerId, date);
      if (slots && slots.length > 0) {
        populateSlotSelect(slots);
      } else {
        const option = document.createElement("option");
        option.disabled = true;
        option.textContent = "Keine Zeitslots für dieses Datum verfügbar";
        slotSelect.appendChild(option);
      }
    } catch (error) {
      console.error("Error fetching slots:", error);
      const option = document.createElement("option");
      option.disabled = true;
      option.textContent = "Fehler beim Laden der Zeitslots";
      slotSelect.appendChild(option);
    }
  }
}

export async function fetchBookings() {
  const { getApiUrl } = getRoutingUtils();
  try {
    const response = await fetch(getApiUrl("my-bookings"), {
      method: "GET",
      headers: { "Content-Type": "application/json" },
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.error || "Failed to fetch bookings");
    }

    return await response.json();
  } catch (error) {
    console.error("Error fetching bookings:", error);
    return [];
  }
}

export async function cancelBooking(appointmentId) {
  const { getApiUrl } = getRoutingUtils();
  try {
    const response = await fetch(
      getApiUrl(`my-bookings/cancel/${appointmentId}`),
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
      }
    );

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.error || "Failed to cancel booking");
    }

    updateBookingsCountDisplay();

    return await response.json();
  } catch (error) {
    console.error("Error cancelling booking:", error);
    throw error;
  }
}

function formatDate(dateString) {
  const date = new Date(dateString);
  return (
    date.toLocaleDateString("de-DE") +
    " " +
    date.toLocaleTimeString("de-DE", { hour: "2-digit", minute: "2-digit" })
  );
}

export function displayQRCode(qrCodeData) {
  const qrSection = document.getElementById("qr-code-display");
  const qrCodeDiv = document.getElementById("qr-code");
  const qrInfoDiv = document.querySelector(".qr-code-preview");

  qrCodeDiv.innerHTML = "";

  if (qrInfoDiv) {
    qrInfoDiv.innerHTML = "";
  }

  new QRCode(qrCodeDiv, {
    text: qrCodeData,
    width: 200,
    height: 200,
    colorDark: "#000000",
    colorLight: "#ffffff",
    correctLevel: QRCode.CorrectLevel.H,
  });

  const previewDiv = document.querySelector(".qr-code-preview");
  if (previewDiv) {
    previewDiv.textContent = qrCodeData;
  } else {
    const newPreviewDiv = document.createElement("pre");
    newPreviewDiv.className = "qr-code-preview";
    newPreviewDiv.textContent = qrCodeData;

    newPreviewDiv.style.backgroundColor = "#f5f5f5";
    newPreviewDiv.style.border = "1px solid #ddd";
    newPreviewDiv.style.borderRadius = "5px";
    newPreviewDiv.style.padding = "10px";
    newPreviewDiv.style.margin = "15px 0";
    newPreviewDiv.style.fontSize = "0.9rem";
    newPreviewDiv.style.whiteSpace = "pre-wrap";
    newPreviewDiv.style.maxHeight = "200px";
    newPreviewDiv.style.overflowY = "auto";

    qrCodeDiv.parentNode.insertBefore(newPreviewDiv, qrCodeDiv.nextSibling);
  }

  qrSection.style.display = "flex";

  document.getElementById("close-qr").addEventListener("click", () => {
    qrSection.style.display = "none";
  });

  const instructionsDiv = document.querySelector(".qr-code-instructions");
  if (!instructionsDiv) {
    const newInstructionsDiv = document.createElement("div");
    newInstructionsDiv.className = "qr-code-instructions";
    newInstructionsDiv.innerHTML = `
      <p><strong>So verwenden Sie diesen QR-Code:</strong></p>
      <ol>
        <li>Öffnen Sie die Kamera-App auf Ihrem Smartphone</li>
        <li>Scannen Sie den QR-Code</li>
        <li>Die Informationen werden direkt auf Ihrem Telefon angezeigt</li>
        <li>Zeigen Sie diesen Code beim Impftermin vor</li>
      </ol>
      <p>Alternativ können Sie einen Screenshot dieses QR-Codes machen.</p>
    `;

    newInstructionsDiv.style.marginTop = "20px";
    newInstructionsDiv.style.padding = "10px";
    newInstructionsDiv.style.backgroundColor = "#e3f2fd";
    newInstructionsDiv.style.borderRadius = "5px";

    const infoDiv = document.querySelector(".qr-code-info");
    if (infoDiv) {
      infoDiv.appendChild(newInstructionsDiv);
    }
  }
}

function createBookingCard(booking) {
  const bookingDate = formatDate(booking.startTime);
  const endTime = new Date(booking.endTime).toLocaleTimeString("de-DE", {
    hour: "2-digit",
    minute: "2-digit",
  });

  const now = new Date();
  const appointmentTime = new Date(booking.startTime);
  const oneHourFromNow = new Date(now.getTime() + 60 * 60 * 1000);
  const isCancellable = appointmentTime > oneHourFromNow;

  let statusClass = "";
  let statusText = "";

  switch (booking.status) {
    case "BOOKED":
      statusClass = "status-booked";
      statusText = "Gebucht";
      break;
    case "CANCELLED":
      statusClass = "status-cancelled";
      statusText = "Storniert";
      break;
    case "COMPLETED":
      statusClass = "status-completed";
      statusText = "Abgeschlossen";
      break;
    default:
      statusClass = "";
      statusText = booking.status;
  }

  const card = document.createElement("div");
  card.className = `booking-card ${statusClass}`;
  card.dataset.id = booking.id;

  const timeUntilAppointment = getTimeUntilAppointment(appointmentTime);

  card.innerHTML = `
    <div class="booking-header">
      <h3>${booking.centerName}</h3>
      <span class="booking-status ${statusClass}">${statusText}</span>
    </div>
    <div class="booking-details">
      <p><strong>Datum:</strong> ${bookingDate} - ${endTime}</p>
      <p><strong>Impfstoff:</strong> ${booking.vaccineName}</p>
      ${
        booking.status === "BOOKED"
          ? `<p class="time-until-appointment ${timeUntilAppointment.cssClass}">
               <strong>Zeit bis zum Termin:</strong> ${
                 timeUntilAppointment.text
               }
             </p>
             <div class="booking-actions">
               ${
                 isCancellable
                   ? `<button class="cancel-booking-btn" data-id="${booking.id}">Termin stornieren</button>`
                   : `<p class="cancellation-disabled">Stornierung nicht mehr möglich (< 1 Stunde vor Termin)</p>`
               }
               <button class="show-qr-btn" data-qr="${
                 booking.qrCode
               }">QR-Code anzeigen</button>
             </div>`
          : booking.status === "CANCELLED"
          ? `<p class="cancelled-info">Storniert am ${
              booking.cancelledAt
                ? formatDate(booking.cancelledAt)
                : "Unbekannt"
            }</p>`
          : ""
      }
    </div>
  `;

  return card;
}

function getTimeUntilAppointment(appointmentTime) {
  const now = new Date();
  const diffMs = appointmentTime - now;

  if (diffMs < 0) {
    return { text: "Termin bereits vorbei", cssClass: "time-past" };
  }

  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
  const diffHours = Math.floor(
    (diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)
  );
  const diffMinutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));

  let text = "";
  let cssClass = "";

  if (diffDays > 0) {
    text = `${diffDays} Tage, ${diffHours} Stunden`;
    cssClass = "time-days";
  } else if (diffHours > 3) {
    text = `${diffHours} Stunden, ${diffMinutes} Minuten`;
    cssClass = "time-hours";
  } else if (diffHours > 1) {
    text = `${diffHours} Stunden, ${diffMinutes} Minuten`;
    cssClass = "time-few-hours";
  } else {
    text = `${diffHours} Stunden, ${diffMinutes} Minuten`;
    cssClass = "time-critical";
  }

  return { text, cssClass };
}

export function displayBookings(bookings) {
  const bookingsContainer = document.getElementById("bookings-list");
  const loader = document.getElementById("bookings-loader");
  const noBookingsMessage = document.getElementById("no-bookings-message");

  if (loader) {
    loader.style.display = "none";
  }

  if (bookingsContainer) {
    bookingsContainer.innerHTML = "";

    if (bookings.length === 0) {
      noBookingsMessage.style.display = "block";
    } else {
      noBookingsMessage.style.display = "none";

      bookings.sort((a, b) => {
        if (a.status === "BOOKED" && b.status !== "BOOKED") return -1;
        if (a.status !== "BOOKED" && b.status === "BOOKED") return 1;

        return new Date(b.startTime) - new Date(a.startTime);
      });

      bookings.forEach((booking) => {
        const card = createBookingCard(booking);
        bookingsContainer.appendChild(card);
      });

      document.querySelectorAll(".cancel-booking-btn").forEach((button) => {
        button.addEventListener("click", async (e) => {
          e.preventDefault();
          const appointmentId = button.getAttribute("data-id");

          const dialog = document.getElementById("cancellation-confirmation");
          const confirmBtn = document.getElementById(
            "confirm-cancellation-btn"
          );
          const cancelBtn = document.getElementById("cancel-cancellation-btn");

          dialog.style.display = "flex";

          confirmBtn.dataset.appointmentId = appointmentId;

          const newConfirmBtn = confirmBtn.cloneNode(true);
          confirmBtn.parentNode.replaceChild(newConfirmBtn, confirmBtn);

          const newCancelBtn = cancelBtn.cloneNode(true);
          cancelBtn.parentNode.replaceChild(newCancelBtn, cancelBtn);

          newConfirmBtn.addEventListener("click", async () => {
            try {
              const id = newConfirmBtn.dataset.appointmentId;
              dialog.style.display = "none";

              const card = document.querySelector(
                `.booking-card[data-id="${id}"]`
              );
              if (card) {
                const actionsDiv = card.querySelector(".booking-actions");
                if (actionsDiv) {
                  actionsDiv.innerHTML =
                    '<div class="loader">Storniere Termin...</div>';
                }
              }

              await cancelBooking(id);
              alert("Termin wurde erfolgreich storniert.");
              initBookingsPage();
            } catch (error) {
              alert("Fehler beim Stornieren des Termins: " + error.message);
              initBookingsPage();
            }
          });

          newCancelBtn.addEventListener("click", () => {
            dialog.style.display = "none";
          });

          dialog.addEventListener("click", (e) => {
            if (e.target === dialog) {
              dialog.style.display = "none";
            }
          });
        });
      });

      document.querySelectorAll(".show-qr-btn").forEach((button) => {
        button.addEventListener("click", (e) => {
          e.preventDefault();
          const qrCode = button.getAttribute("data-qr");

          displayQRCode(qrCode);
        });
      });
    }
  }
}

export async function initBookingsPage() {
  const isLoggedIn = await checkSessionStatus();

  if (!isLoggedIn) {
    window.location.href = "/login";
    return;
  }

  const bookings = await fetchBookings();
  displayBookings(bookings);
}
