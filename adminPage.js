let currentAppointments = [];
let selectedAppointmentId = null;
let adminCenterId = null;
let adminCenterName = "";

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

export function initAdminPage(centerId) {
  console.log("Initializing admin page with center ID:", centerId);
  adminCenterId = centerId;

  fetchCenterName(centerId);

  fetchAppointments();

  setupEventListeners();

  setupTabFunctionality();

  setupVaccineManagement();
}

async function fetchCenterName(centerId) {
  const { getApiUrl } = getRoutingUtils();
  try {
    const response = await fetch(getApiUrl("centers"), {
      method: "GET",
      headers: { "Content-Type": "application/json" },
    });

    if (!response.ok) {
      throw new Error("Failed to fetch centers");
    }

    const centers = await response.json();
    const center = centers.find((c) => c.id === centerId);

    if (center) {
      adminCenterName = center.name;
      document.getElementById("admin-center-name").textContent =
        adminCenterName;
    }
  } catch (error) {
    console.error("Error fetching center name:", error);
  }
}

async function fetchAppointments() {
  const { getApiUrl } = getRoutingUtils();
  showLoading(true);

  try {
    const response = await fetch(getApiUrl("admin/appointments"), {
      method: "GET",
      headers: { "Content-Type": "application/json" },
    });

    if (!response.ok) {
      throw new Error("Failed to fetch appointments");
    }

    currentAppointments = await response.json();
    console.log("Fetched appointments:", currentAppointments);
    updateAppointmentsTable(currentAppointments);
    updateStatistics(currentAppointments);
  } catch (error) {
    console.error("Error fetching appointments:", error);
    showError("Fehler beim Laden der Termine");
  } finally {
    showLoading(false);
  }
}

function updateAppointmentsTable(appointments) {
  const tableBody = document.getElementById("admin-appointments-list");
  if (!tableBody) {
    console.error("Could not find admin-appointments-list element");
    return;
  }

  tableBody.innerHTML = "";

  if (!appointments || appointments.length === 0) {
    const noResultsRow = document.createElement("tr");
    noResultsRow.className = "no-results-row";
    noResultsRow.innerHTML = '<td colspan="7">Keine Termine gefunden</td>';
    tableBody.appendChild(noResultsRow);
    return;
  }

  appointments.forEach((appointment) => {
    console.log("Processing appointment:", appointment);

    const row = document.createElement("tr");

    let formattedDate = "N/A";
    let formattedTime = "N/A";

    if (appointment.startTime) {
      try {
        const startDate = new Date(appointment.startTime);
        formattedDate = startDate.toLocaleDateString("de-DE");
        formattedTime = startDate.toLocaleTimeString("de-DE", {
          hour: "2-digit",
          minute: "2-digit",
        });
      } catch (e) {
        console.error("Error formatting date:", e);
      }
    }

    let statusClass = "";
    let statusText = "";

    switch (appointment.status) {
      case "BOOKED":
        statusClass = "booked";
        statusText = "Gebucht";
        break;
      case "CANCELLED":
        statusClass = "cancelled";
        statusText = "Storniert";
        break;
      case "COMPLETED":
        statusClass = "completed";
        statusText = "Abgeschlossen";
        break;
      default:
        statusText = appointment.status || "Unbekannt";
    }

    let patientEmail = "Unknown";
    try {
      if (
        appointment.additionalInfo &&
        appointment.additionalInfo.patientEmail
      ) {
        patientEmail = appointment.additionalInfo.patientEmail;
      } else if (
        appointment.getAllAdditionalInfo &&
        typeof appointment.getAllAdditionalInfo === "function"
      ) {
        const allInfo = appointment.getAllAdditionalInfo();
        if (allInfo && allInfo.patientEmail) {
          patientEmail = allInfo.patientEmail;
        }
      } else if (appointment.patientEmail) {
        patientEmail = appointment.patientEmail;
      }
    } catch (e) {
      console.error("Error getting patient email:", e);
    }

    row.innerHTML = `
      <td>${appointment.id || "N/A"}</td>
      <td>${patientEmail}</td>
      <td>${formattedDate}</td>
      <td>${formattedTime}</td>
      <td>${appointment.vaccineName || "N/A"}</td>
      <td><span class="appointment-status ${statusClass}">${statusText}</span></td>
      <td class="appointment-actions">
        <button class="action-button view" data-id="${
          appointment.id
        }">Details</button>
        ${
          appointment.status === "BOOKED"
            ? `<button class="action-button cancel" data-id="${appointment.id}">Stornieren</button>`
            : ""
        }
      </td>
    `;

    tableBody.appendChild(row);
  });

  attachButtonEventListeners();
}

function updateStatistics(appointments) {
  if (!appointments) {
    return;
  }

  const totalCount = appointments.length;
  const openCount = appointments.filter(
    (a) => a && a.status === "BOOKED"
  ).length;
  const cancelledCount = appointments.filter(
    (a) => a && a.status === "CANCELLED"
  ).length;

  const totalElement = document.getElementById("total-appointments");
  const openElement = document.getElementById("open-appointments");
  const cancelledElement = document.getElementById("cancelled-appointments");

  if (totalElement) totalElement.textContent = totalCount;
  if (openElement) openElement.textContent = openCount;
  if (cancelledElement) cancelledElement.textContent = cancelledCount;
}

function setupEventListeners() {
  const statusFilter = document.getElementById("status-filter");
  if (statusFilter) {
    statusFilter.addEventListener("change", applyFilters);
  }

  const dateFilter = document.getElementById("date-filter");
  if (dateFilter) {
    dateFilter.addEventListener("change", applyFilters);
  }

  const clearFiltersBtn = document.getElementById("clear-filters");
  if (clearFiltersBtn) {
    clearFiltersBtn.addEventListener("click", clearFilters);
  }

  document
    .querySelectorAll(".admin-modal-close, .admin-modal-close-btn")
    .forEach((button) => {
      button.addEventListener("click", () => {
        const detailsModal = document.getElementById(
          "admin-appointment-details"
        );
        if (detailsModal) {
          detailsModal.style.display = "none";
        }
      });
    });

  const detailCancelBtn = document.getElementById("detail-cancel-btn");
  if (detailCancelBtn) {
    detailCancelBtn.addEventListener("click", () => {
      const detailsModal = document.getElementById("admin-appointment-details");
      const confirmModal = document.getElementById("confirm-cancel-modal");

      if (detailsModal) detailsModal.style.display = "none";
      if (confirmModal) confirmModal.style.display = "flex";
    });
  }

  const confirmCancelBtn = document.getElementById("confirm-cancel-btn");
  if (confirmCancelBtn) {
    confirmCancelBtn.addEventListener("click", () => {
      cancelAppointment(selectedAppointmentId);
    });
  }

  const cancelModalCloseBtn = document.querySelector(".cancel-modal-close");
  if (cancelModalCloseBtn) {
    cancelModalCloseBtn.addEventListener("click", () => {
      const confirmModal = document.getElementById("confirm-cancel-modal");
      if (confirmModal) confirmModal.style.display = "none";
    });
  }

  document.querySelectorAll(".admin-modal").forEach((modal) => {
    modal.addEventListener("click", (e) => {
      if (e.target === modal) {
        modal.style.display = "none";
      }
    });
  });
}

function attachButtonEventListeners() {
  document.querySelectorAll(".action-button.view").forEach((button) => {
    button.addEventListener("click", () => {
      const appointmentId = parseInt(button.getAttribute("data-id"), 10);
      showAppointmentDetails(appointmentId);
    });
  });

  document.querySelectorAll(".action-button.cancel").forEach((button) => {
    button.addEventListener("click", () => {
      const appointmentId = parseInt(button.getAttribute("data-id"), 10);
      selectedAppointmentId = appointmentId;
      const confirmModal = document.getElementById("confirm-cancel-modal");
      if (confirmModal) confirmModal.style.display = "flex";
    });
  });
}

function showAppointmentDetails(appointmentId) {
  selectedAppointmentId = appointmentId;
  const appointment = currentAppointments.find((a) => a.id === appointmentId);

  if (!appointment) {
    console.error("Appointment not found:", appointmentId);
    return;
  }

  const detailsModal = document.getElementById("admin-appointment-details");
  if (!detailsModal) {
    console.error("Details modal not found");
    return;
  }

  let formattedDateTime = "N/A";
  let formattedCreatedDate = "N/A";

  if (appointment.startTime) {
    try {
      const startDate = new Date(appointment.startTime);
      formattedDateTime = `${startDate.toLocaleDateString(
        "de-DE"
      )} ${startDate.toLocaleTimeString("de-DE", {
        hour: "2-digit",
        minute: "2-digit",
      })}`;
    } catch (e) {
      console.error("Error formatting start date:", e);
    }
  }

  if (appointment.createdAt) {
    try {
      const createdDate = new Date(appointment.createdAt);
      formattedCreatedDate = `${createdDate.toLocaleDateString(
        "de-DE"
      )} ${createdDate.toLocaleTimeString("de-DE", {
        hour: "2-digit",
        minute: "2-digit",
      })}`;
    } catch (e) {
      console.error("Error formatting created date:", e);
    }
  }

  let patientEmail = "Unknown";
  try {
    if (appointment.additionalInfo && appointment.additionalInfo.patientEmail) {
      patientEmail = appointment.additionalInfo.patientEmail;
    } else if (
      appointment.getAllAdditionalInfo &&
      typeof appointment.getAllAdditionalInfo === "function"
    ) {
      const allInfo = appointment.getAllAdditionalInfo();
      if (allInfo && allInfo.patientEmail) {
        patientEmail = allInfo.patientEmail;
      }
    } else if (appointment.patientEmail) {
      patientEmail = appointment.patientEmail;
    }
  } catch (e) {
    console.error("Error getting patient email:", e);
  }

  const detailIdElement = document.getElementById("detail-id");
  const detailPatientElement = document.getElementById("detail-patient");
  const detailDatetimeElement = document.getElementById("detail-datetime");
  const detailVaccineElement = document.getElementById("detail-vaccine");
  const detailStatusElement = document.getElementById("detail-status");
  const detailCreatedElement = document.getElementById("detail-created");

  if (detailIdElement) detailIdElement.textContent = appointment.id;
  if (detailPatientElement) detailPatientElement.textContent = patientEmail;
  if (detailDatetimeElement)
    detailDatetimeElement.textContent = formattedDateTime;
  if (detailVaccineElement)
    detailVaccineElement.textContent = appointment.vaccineName || "N/A";

  if (detailStatusElement) {
    detailStatusElement.textContent = getStatusText(appointment.status);
    detailStatusElement.className =
      "detail-value " + getStatusClass(appointment.status);
  }

  if (detailCreatedElement)
    detailCreatedElement.textContent = formattedCreatedDate;

  const cancelledInfo = document.querySelector(".detail-row.cancelled-info");
  if (cancelledInfo) {
    if (appointment.status === "CANCELLED" && appointment.cancelledAt) {
      try {
        const cancelledDate = new Date(appointment.cancelledAt);
        const formattedCancelledDate = `${cancelledDate.toLocaleDateString(
          "de-DE"
        )} ${cancelledDate.toLocaleTimeString("de-DE", {
          hour: "2-digit",
          minute: "2-digit",
        })}`;
        const detailCancelledElement =
          document.getElementById("detail-cancelled");
        if (detailCancelledElement)
          detailCancelledElement.textContent = formattedCancelledDate;
        cancelledInfo.style.display = "flex";
      } catch (e) {
        console.error("Error formatting cancelled date:", e);
        cancelledInfo.style.display = "none";
      }
    } else {
      cancelledInfo.style.display = "none";
    }
  }

  const cancelBtn = document.getElementById("detail-cancel-btn");
  if (cancelBtn) {
    if (appointment.status === "BOOKED") {
      cancelBtn.style.display = "block";
    } else {
      cancelBtn.style.display = "none";
    }
  }

  const qrContainer = document.getElementById("detail-qr-code");
  if (qrContainer) {
    qrContainer.innerHTML = "";

    if (appointment.qrCode) {
      try {
        if (typeof QRCode === "function") {
          new QRCode(qrContainer, {
            text: appointment.qrCode,
            width: 128,
            height: 128,
            colorDark: "#000000",
            colorLight: "#ffffff",
            correctLevel: QRCode.CorrectLevel.H,
          });
        } else {
          qrContainer.textContent = "QR Code library not available";
          console.error("QRCode library not loaded");
        }
      } catch (e) {
        console.error("Error generating QR code:", e);
        qrContainer.textContent = "Error generating QR code";
      }
    } else {
      qrContainer.textContent = "Kein QR-Code verfügbar";
    }
  }

  detailsModal.style.display = "flex";
}

async function cancelAppointment(appointmentId) {
  const { getApiUrl } = getRoutingUtils();
  try {
    const response = await fetch(
      getApiUrl(`admin/appointments/cancel/${appointmentId}`),
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
      }
    );

    if (!response.ok) {
      throw new Error("Failed to cancel appointment");
    }

    const confirmModal = document.getElementById("confirm-cancel-modal");
    if (confirmModal) confirmModal.style.display = "none";

    fetchAppointments();

    alert("Termin wurde erfolgreich storniert");
  } catch (error) {
    console.error("Error cancelling appointment:", error);
    alert("Fehler beim Stornieren des Termins: " + error.message);
    const confirmModal = document.getElementById("confirm-cancel-modal");
    if (confirmModal) confirmModal.style.display = "none";
  }
}

function applyFilters() {
  const statusFilter = document.getElementById("status-filter");
  const dateFilter = document.getElementById("date-filter");

  if (!statusFilter || !dateFilter) {
    console.error("Filter elements not found");
    return;
  }

  let filteredAppointments = [...currentAppointments];

  if (statusFilter.value !== "all") {
    filteredAppointments = filteredAppointments.filter(
      (a) => a && a.status === statusFilter.value
    );
  }

  if (dateFilter.value) {
    const filterDate = new Date(dateFilter.value);
    filterDate.setHours(0, 0, 0, 0);

    filteredAppointments = filteredAppointments.filter((a) => {
      if (!a || !a.startTime) return false;

      try {
        const appointmentDate = new Date(a.startTime);
        appointmentDate.setHours(0, 0, 0, 0);
        return appointmentDate.getTime() === filterDate.getTime();
      } catch (e) {
        console.error("Error comparing dates:", e);
        return false;
      }
    });
  }

  updateAppointmentsTable(filteredAppointments);
}

function clearFilters() {
  const statusFilter = document.getElementById("status-filter");
  const dateFilter = document.getElementById("date-filter");

  if (statusFilter) statusFilter.value = "all";
  if (dateFilter) dateFilter.value = "";

  updateAppointmentsTable(currentAppointments);
}

function showLoading(isLoading) {
  const tableBody = document.getElementById("admin-appointments-list");

  if (isLoading && tableBody) {
    tableBody.innerHTML =
      '<tr class="loading-row"><td colspan="7">Termine werden geladen...</td></tr>';
  }
}

function showError(message) {
  const tableBody = document.getElementById("admin-appointments-list");
  if (tableBody) {
    tableBody.innerHTML = `<tr class="error-row"><td colspan="7">${message}</td></tr>`;
  }
}

function getStatusText(status) {
  switch (status) {
    case "BOOKED":
      return "Gebucht";
    case "CANCELLED":
      return "Storniert";
    case "COMPLETED":
      return "Abgeschlossen";
    default:
      return status || "Unbekannt";
  }
}

function getStatusClass(status) {
  switch (status) {
    case "BOOKED":
      return "status-booked";
    case "CANCELLED":
      return "status-cancelled";
    case "COMPLETED":
      return "status-completed";
    default:
      return "";
  }
}

function setupTabFunctionality() {
  const tabButtons = document.querySelectorAll(".admin-tab-button");
  const tabContents = document.querySelectorAll(".admin-tab-content");

  if (
    document.querySelector(".admin-tab-content.active") === null &&
    tabContents.length > 0
  ) {
    tabContents[0].classList.add("active");
  }

  tabButtons.forEach((button) => {
    button.addEventListener("click", () => {
      tabButtons.forEach((btn) => btn.classList.remove("active"));
      tabContents.forEach((content) => (content.style.display = "none"));

      button.classList.add("active");
      const tabName = button.getAttribute("data-tab");
      const targetTab = document.getElementById(`admin-${tabName}`);

      if (targetTab) {
        targetTab.style.display = "block";

        if (
          tabName === "vaccines" &&
          !document.querySelector("#admin-vaccines-list tr")
        ) {
          loadVaccinesData();
        } else if (
          tabName === "timeslots" &&
          !document.querySelector("#admin-timeslots-list tr")
        ) {
          const datePicker = document.getElementById("admin-date-picker");
          if (datePicker) {
            datePicker.valueAsDate = new Date();
          }
        }
      }
    });
  });

  setupTimeslotManagement();
}

async function loadVaccinesData() {
  const { getApiUrl } = getRoutingUtils();
  if (!adminCenterId) {
    console.error("Admin center ID is not set");
    return;
  }

  const vaccinesContainer = document.getElementById("admin-vaccines-list");
  const loader = document.getElementById("vaccines-loader");
  const noVaccinesMessage = document.getElementById("no-vaccines-message");

  if (loader) loader.style.display = "block";
  if (vaccinesContainer) vaccinesContainer.innerHTML = "";
  if (noVaccinesMessage) noVaccinesMessage.style.display = "none";

  try {
    console.log(`Fetching vaccines for center ID: ${adminCenterId}`);
    const response = await fetch(
      getApiUrl(`centers/${adminCenterId}/vaccines`),
      {
        method: "GET",
        headers: { "Content-Type": "application/json" },
      }
    );

    if (!response.ok) {
      throw new Error(
        `Failed to fetch vaccines: ${response.status} ${response.statusText}`
      );
    }

    const vaccines = await response.json();
    console.log(`Fetched ${vaccines.length} vaccines:`, vaccines);

    if (vaccines.length === 0) {
      vaccinesContainer.innerHTML =
        '<tr><td colspan="5">Keine Impfstoffe gefunden</td></tr>';
      if (noVaccinesMessage) noVaccinesMessage.style.display = "block";
    } else {
      vaccines.forEach((vaccine) => {
        const row = document.createElement("tr");
        row.innerHTML = `
          <td>${vaccine.id}</td>
          <td>${vaccine.name}</td>
          <td>${vaccine.details || "-"}</td>
          <td>
            <input type="number" class="vaccine-quantity" data-id="${
              vaccine.id
            }" value="${vaccine.stock}" min="0">
          </td>
          <td>
            <button class="admin-button primary update-vaccine-btn" data-id="${
              vaccine.id
            }">Aktualisieren</button>
          </td>
        `;
        vaccinesContainer.appendChild(row);
      });

      setupVaccineUpdateButtons();
    }
  } catch (error) {
    console.error("Error loading vaccines:", error);
    vaccinesContainer.innerHTML = `<tr><td colspan="5">Fehler beim Laden der Impfstoffe: ${error.message}</td></tr>`;
  } finally {
    if (loader) loader.style.display = "none";
  }
}

function setupVaccineUpdateButtons() {
  const { getApiUrl } = getRoutingUtils();
  console.log("Setting up vaccine update buttons");
  document.querySelectorAll(".update-vaccine-btn").forEach((button) => {
    button.addEventListener("click", async function () {
      const vaccineId = this.getAttribute("data-id");
      const quantityInput = document.querySelector(
        `.vaccine-quantity[data-id="${vaccineId}"]`
      );

      if (!quantityInput || !quantityInput.value) {
        alert("Bitte geben Sie eine gültige Menge ein");
        return;
      }

      const newQuantity = parseInt(quantityInput.value);

      if (isNaN(newQuantity) || newQuantity < 0) {
        alert("Bitte geben Sie eine gültige Menge ein (≥ 0)");
        return;
      }

      this.disabled = true;
      const originalText = this.textContent;
      this.textContent = "Wird aktualisiert...";

      try {
        console.log(
          `Updating vaccine ID ${vaccineId} to quantity ${newQuantity}`
        );
        const response = await fetch(getApiUrl("admin/vaccines"), {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            vaccineId: parseInt(vaccineId),
            quantity: newQuantity,
          }),
        });

        console.log("Update response status:", response.status);

        if (!response.ok) {
          throw new Error(
            `Failed to update vaccine quantity: ${response.status} ${response.statusText}`
          );
        }

        const result = await response.json();
        console.log("Update result:", result);

        if (result.success) {
          this.classList.remove("primary");
          this.classList.add("success");
          this.textContent = "Aktualisiert ✓";

          setTimeout(() => {
            this.classList.remove("success");
            this.classList.add("primary");
            this.textContent = originalText;
            this.disabled = false;
          }, 2000);
        } else {
          alert(
            "Fehler beim Aktualisieren der Impfstoffmenge: " +
              (result.message || "Unbekannter Fehler")
          );
          this.textContent = originalText;
          this.disabled = false;
        }
      } catch (error) {
        console.error("Error updating vaccine quantity:", error);
        alert("Fehler beim Aktualisieren der Impfstoffmenge: " + error.message);
        this.textContent = originalText;
        this.disabled = false;
      }
    });
  });
}

function setupAddVaccineForm() {
  const { getApiUrl } = getRoutingUtils();

  const addVaccineForm = document.getElementById("add-vaccine-form");
  const refreshButton = document.getElementById("refresh-vaccines-btn");

  if (refreshButton) {
    console.log("Setting up refresh vaccines button");
    refreshButton.addEventListener("click", () => {
      console.log("Refreshing vaccines data");
      loadVaccinesData();
    });
  }

  if (addVaccineForm) {
    console.log("Setting up add vaccine form submission");

    const newForm = addVaccineForm.cloneNode(true);
    addVaccineForm.parentNode.replaceChild(newForm, addVaccineForm);

    newForm.addEventListener("submit", async function (event) {
      event.preventDefault();
      console.log("Add vaccine form submitted");

      const name = document.getElementById("new-vaccine-name").value;
      const details = document.getElementById("new-vaccine-details").value;
      const quantity = parseInt(
        document.getElementById("new-vaccine-quantity").value
      );

      if (!name || name.trim() === "") {
        alert("Bitte geben Sie einen Namen für den Impfstoff ein");
        return;
      }

      if (isNaN(quantity) || quantity < 0) {
        alert("Bitte geben Sie eine gültige Menge ein (≥ 0)");
        return;
      }

      const submitButton = newForm.querySelector("button[type='submit']");
      submitButton.disabled = true;
      const originalText = submitButton.textContent;
      submitButton.textContent = "Wird hinzugefügt...";

      try {
        console.log(`Adding new vaccine: ${name}, quantity: ${quantity}`);

        const requestData = {
          name: name,
          details: details || "",
          quantity: quantity,
        };

        console.log("Request payload:", requestData);

        const response = await fetch(getApiUrl("admin/vaccines?action=new"), {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(requestData),
        });

        console.log("Response status:", response.status);

        const responseText = await response.text();
        console.log("Response text:", responseText);

        let result;
        try {
          result = JSON.parse(responseText);
        } catch (e) {
          console.error("Error parsing response:", e);
          throw new Error(`Failed to add new vaccine: ${responseText}`);
        }

        if (result.success) {
          alert("Impfstoff erfolgreich hinzugefügt");
          newForm.reset();
          loadVaccinesData();
        } else {
          alert(
            "Fehler beim Hinzufügen des Impfstoffs: " +
              (result.message || "Unbekannter Fehler")
          );
        }
      } catch (error) {
        console.error("Error adding vaccine:", error);
        alert("Fehler beim Hinzufügen des Impfstoffs: " + error.message);
      } finally {
        submitButton.textContent = originalText;
        submitButton.disabled = false;
      }
    });
  } else {
    console.error("Add vaccine form not found");
  }
}

function setupVaccineManagement() {
  console.log("Setting up vaccine management");
  loadVaccinesData();
  setupAddVaccineForm();
}

function setupTimeslotManagement() {
  const { getApiUrl } = getRoutingUtils();
  const loadTimeslotsBtn = document.getElementById("load-timeslots");
  if (loadTimeslotsBtn) {
    loadTimeslotsBtn.addEventListener("click", loadTimeslots);
  }

  const addTimeslotForm = document.getElementById("add-timeslot-form");
  if (addTimeslotForm) {
    addTimeslotForm.addEventListener("submit", async function (event) {
      event.preventDefault();

      const startTime = document.getElementById("new-timeslot-start").value;
      const endTime = document.getElementById("new-timeslot-end").value;
      const capacity = document.getElementById("new-timeslot-capacity").value;

      if (!startTime || !endTime || !capacity) {
        alert("Bitte füllen Sie alle Felder aus");
        return;
      }

      if (new Date(startTime) >= new Date(endTime)) {
        alert("Die Startzeit muss vor der Endzeit liegen");
        return;
      }

      if (parseInt(capacity) <= 0) {
        alert("Die Kapazität muss größer als 0 sein");
        return;
      }

      try {
        const response = await fetch(getApiUrl("admin/timeslots"), {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            startTime: new Date(startTime).toISOString(),
            endTime: new Date(endTime).toISOString(),
            capacity: parseInt(capacity),
          }),
        });

        if (!response.ok) {
          throw new Error("Failed to create timeslot");
        }

        const result = await response.json();

        if (result.success) {
          alert("Zeitslot erfolgreich erstellt");
          addTimeslotForm.reset();
          loadTimeslots();
        } else {
          alert(
            "Fehler beim Erstellen des Zeitslots: " +
              (result.message || "Unbekannter Fehler")
          );
        }
      } catch (error) {
        console.error("Error creating timeslot:", error);
        alert("Fehler beim Erstellen des Zeitslots: " + error.message);
      }
    });
  }
}

async function loadTimeslots() {
  const { getApiUrl } = getRoutingUtils();
  if (!adminCenterId) return;

  const datePicker = document.getElementById("admin-date-picker");
  if (!datePicker || !datePicker.value) {
    alert("Bitte wählen Sie ein Datum");
    return;
  }

  const date = datePicker.value;
  const timeslotsContainer = document.getElementById("admin-timeslots-list");
  const loader = document.getElementById("timeslots-loader");

  if (loader) loader.style.display = "block";
  if (timeslotsContainer) timeslotsContainer.innerHTML = "";

  try {
    const response = await fetch(
      getApiUrl(`slots?centerId=${adminCenterId}&date=${date}`),
      {
        method: "GET",
        headers: { "Content-Type": "application/json" },
      }
    );

    if (!response.ok) {
      throw new Error("Failed to fetch timeslots");
    }

    const timeslots = await response.json();

    if (timeslots.length === 0) {
      timeslotsContainer.innerHTML =
        '<tr><td colspan="5">Keine Zeitslots für dieses Datum gefunden</td></tr>';
    } else {
      timeslots.forEach((slot) => {
        const startTime = new Date(slot.startTime);
        const endTime = new Date(slot.endTime);

        const formattedStart = startTime.toLocaleTimeString("de-DE", {
          hour: "2-digit",
          minute: "2-digit",
        });

        const formattedEnd = endTime.toLocaleTimeString("de-DE", {
          hour: "2-digit",
          minute: "2-digit",
        });

        const row = document.createElement("tr");
        row.innerHTML = `
          <td>${formattedStart}</td>
          <td>${formattedEnd}</td>
          <td>${slot.capacity}</td>
          <td>${slot.bookedCount} / ${slot.capacity}</td>
          <td>
            <button class="admin-button secondary edit-capacity-btn" data-id="${
              slot.id
            }">Kapazität ändern</button>
            ${
              slot.bookedCount === 0
                ? `<button class="admin-button danger delete-slot-btn" data-id="${slot.id}">Löschen</button>`
                : ""
            }
          </td>
        `;
        timeslotsContainer.appendChild(row);
      });

      setupTimeslotButtons();
    }
  } catch (error) {
    console.error("Error loading timeslots:", error);
    timeslotsContainer.innerHTML = `<tr><td colspan="5">Fehler beim Laden der Zeitslots: ${error.message}</td></tr>`;
  } finally {
    if (loader) loader.style.display = "none";
  }
}

function setupTimeslotButtons() {
  const { getApiUrl } = getRoutingUtils();
  document.querySelectorAll(".edit-capacity-btn").forEach((button) => {
    button.addEventListener("click", async function () {
      const slotId = this.getAttribute("data-id");
      const newCapacity = prompt("Neue Kapazität eingeben:");

      if (newCapacity === null) return;

      const capacity = parseInt(newCapacity);

      if (isNaN(capacity) || capacity <= 0) {
        alert("Bitte geben Sie eine gültige Kapazität ein (> 0)");
        return;
      }

      try {
        const response = await fetch(getApiUrl(`admin/timeslots/${slotId}`), {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ capacity }),
        });

        if (!response.ok) {
          throw new Error("Failed to update timeslot");
        }

        const result = await response.json();

        if (result.success) {
          alert("Kapazität erfolgreich aktualisiert");
          loadTimeslots();
        } else {
          alert(
            "Fehler beim Aktualisieren der Kapazität: " +
              (result.message || "Unbekannter Fehler")
          );
        }
      } catch (error) {
        console.error("Error updating timeslot:", error);
        alert("Fehler beim Aktualisieren der Kapazität: " + error.message);
      }
    });
  });

  document.querySelectorAll(".delete-slot-btn").forEach((button) => {
    button.addEventListener("click", async function () {
      const slotId = this.getAttribute("data-id");

      if (
        !confirm("Sind Sie sicher, dass Sie diesen Zeitslot löschen möchten?")
      ) {
        return;
      }

      try {
        const response = await fetch(getApiUrl(`admin/timeslots/${slotId}`), {
          method: "DELETE",
          headers: { "Content-Type": "application/json" },
        });

        if (!response.ok) {
          throw new Error("Failed to delete timeslot");
        }

        const result = await response.json();

        if (result.success) {
          alert("Zeitslot erfolgreich gelöscht");
          loadTimeslots();
        } else {
          alert(
            "Fehler beim Löschen des Zeitslots: " +
              (result.message || "Unbekannter Fehler")
          );
        }
      } catch (error) {
        console.error("Error deleting timeslot:", error);
        alert("Fehler beim Löschen des Zeitslots: " + error.message);
      }
    });
  });
}

export { fetchAppointments, updateAppointmentsTable, updateStatistics };
