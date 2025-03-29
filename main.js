import {
  loginUser,
  registerUser,
  updateNavbarBasedOnSession,
  showViewAsync,
  showView,
  updateBookingsCountDisplay,
  checkSessionStatus,
  checkAdminStatus,
} from "./fetch.js";
import {
  bookingPage,
  initBookingsPage,
  populateCenters,
  updateSlotsForSelectedDate,
} from "./bookingPage.js";
import { initAdminPage } from "./adminPage.js";

const CONTEXTPATH = "/docker-swe3-2024-14-java/";

function getRouteFromPath(path) {
  return path.replace(CONTEXTPATH, "").replace(/^\//, "") || "hero";
}

function getFullPath(route) {
  return CONTEXTPATH + (route.startsWith("/") ? route.substring(1) : route);
}

function getApiUrl(endpoint) {
  const [path, queryString] = endpoint.split("?");

  const apiPath = path.startsWith("api/") ? path.substring(4) : path;

  const baseUrl = CONTEXTPATH + "api/" + apiPath;

  return queryString ? `${baseUrl}?${queryString}` : baseUrl;
}

window.routingUtils = {
  CONTEXTPATH,
  getRouteFromPath,
  getFullPath,
  getApiUrl,
};

let isUserAdmin = false;
let adminCenterId = null;

document.addEventListener("DOMContentLoaded", async function () {
  console.log("Page loaded with path:", window.location.pathname);

  const isLoggedIn = await checkSessionStatus();
  console.log("User logged in:", isLoggedIn);

  let adminStatus = { isAdmin: false, centerId: null };

  if (isLoggedIn) {
    adminStatus = await checkAdminStatus();
    isUserAdmin = adminStatus.isAdmin;
    adminCenterId = adminStatus.centerId;
    console.log("Admin status:", isUserAdmin, "Center ID:", adminCenterId);
  }

  const fullPath = window.location.pathname;
  console.log("Full path:", fullPath);
  let initialView = getRouteFromPath(fullPath);
  console.log("Initial view derived from path:", initialView);

  if (initialView === "admin") {
    if (!isUserAdmin) {
      initialView = "login";
      history.replaceState({ view: "login" }, "", getFullPath("login"));
      console.log("Not admin, redirecting to login");
    }
  } else if (isUserAdmin && (initialView === "hero" || initialView === "")) {
    initialView = "admin";
    history.replaceState({ view: "admin" }, "", getFullPath("admin"));
    console.log("Admin logged in at home, redirecting to admin");
  }

  console.log("Showing initial view:", initialView);
  await showViewAsync(initialView);

  await updateNavbarBasedOnSession();

  if (initialView === "admin" && isUserAdmin && adminCenterId) {
    console.log("Initializing admin page with center ID:", adminCenterId);
    initAdminPage(adminCenterId);

    setTimeout(() => {
      if (fullPath.includes("timeslots")) {
        const timeslotsTabBtn = document.querySelector(
          '.admin-tab-button[data-tab="timeslots"]'
        );
        if (timeslotsTabBtn) {
          timeslotsTabBtn.click();

          const datePicker = document.getElementById("admin-date-picker");
          if (datePicker) {
            datePicker.valueAsDate = new Date();
            const loadBtn = document.getElementById("load-timeslots");
            if (loadBtn) loadBtn.click();
          }
        }
      } else if (fullPath.includes("vaccines")) {
        const vaccinesTabBtn = document.querySelector(
          '.admin-tab-button[data-tab="vaccines"]'
        );
        if (vaccinesTabBtn) vaccinesTabBtn.click();
      }
    }, 100);
  } else if (initialView === "appointments") {
    console.log("Initializing appointments page");
    bookingPage();
    await populateCenters();
    updateBookingsCountDisplay();
  } else if (initialView === "my-bookings") {
    console.log("Initializing bookings page");
    initBookingsPage();
  } else {
    bookingPage();
    populateCenters();
  }

  function showHeroContent() {
    const currentView = getRouteFromPath(window.location.pathname);
    console.log("Current view for hero content:", currentView);

    const mainContent = document.getElementById("hero-main-content");
    if (mainContent) {
      if (currentView === "hero" || currentView === "") {
        mainContent.classList.add("active");
      } else {
        mainContent.classList.remove("active");
      }
    }
  }

  showHeroContent();

  function updateNavbarForAdmin() {
    const navbar = document.querySelector(".nav");

    const existingAdminLink = document.querySelector(".nav-link.admin-link");
    if (existingAdminLink) {
      existingAdminLink.remove();
    }

    if (isUserAdmin) {
      const adminLink = document.createElement("a");
      adminLink.href = getFullPath("admin");
      adminLink.className = "nav-link admin-link";
      adminLink.textContent = "Admin-Bereich";
      adminLink.style.color = "#ffeb3b";

      const logoutLink = document.getElementById("logout");
      if (navbar && logoutLink) {
        navbar.insertBefore(adminLink, logoutLink);

        adminLink.addEventListener("click", function (e) {
          e.preventDefault();
          history.pushState({ view: "admin" }, "", getFullPath("admin"));
          showView("admin");

          if (adminCenterId) {
            initAdminPage(adminCenterId);
          }
        });
      }
    }
  }

  const centerSelect = document.getElementById("centerSelect");
  if (centerSelect) {
    centerSelect.addEventListener("change", updateSlotsForSelectedDate);
  }

  const dateInput = document.getElementById("appointmentDate");
  if (dateInput) {
    dateInput.addEventListener("change", updateSlotsForSelectedDate);

    const today = new Date().toISOString().split("T")[0];
    dateInput.min = today;
  }

  const appointmentForm = document.getElementById("appointmentForm");
  if (appointmentForm) {
    appointmentForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      const centerId = document.getElementById("centerSelect").value;
      const appointmentDate = document.getElementById("appointmentDate").value;
      const slotId = document.getElementById("slotSelect").value;
      const bookingFor = document.getElementById("bookingFor").value;
      const vaccineId = document.getElementById("vaccineSelect").value;

      if (!centerId || !appointmentDate || !slotId || !vaccineId) {
        alert("Bitte wählen Sie alle erforderlichen Felder aus.");
        return;
      }

      let bookingData = {
        centerId: parseInt(centerId),
        appointmentDate,
        slotId: parseInt(slotId),
        bookingFor,
        vaccineId: parseInt(vaccineId),
      };

      if (bookingFor === "other") {
        const numPersonsSelect = document.getElementById("numPersons");
        const patientCount = parseInt(numPersonsSelect.value);

        if (isNaN(patientCount) || patientCount < 1 || patientCount > 5) {
          alert("Bitte Anzahl der Personen wählen (1-5).");
          return;
        }

        bookingData.patientCount = patientCount;
      }

      try {
        const response = await fetch(getApiUrl("bookAppointment"), {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(bookingData),
        });

        const result = await response.json();
        if (result.success) {
          alert("Termin erfolgreich gebucht!");
          updateBookingsCountDisplay();
          history.pushState(
            { view: "my-bookings" },
            "",
            getFullPath("my-bookings")
          );
          await showViewAsync("my-bookings");
        } else {
          alert("Buchung fehlgeschlagen: " + result.message);
        }
      } catch (error) {
        alert("Fehler beim Buchen des Termins: " + error.message);
      }
    });
  }

  const registerForm = document.getElementById("registerForm");
  if (registerForm) {
    registerForm.addEventListener("submit", async function (e) {
      e.preventDefault();
      const email = document.getElementById("regEmail").value;
      const password = document.getElementById("regPassword").value;
      const confirmPassword =
        document.getElementById("regConfirmPassword").value;

      if (password !== confirmPassword) {
        alert("Passwörter stimmen nicht überein");
        return;
      }

      const registerData = { email, password };

      try {
        const result = await registerUser(registerData);
        if (result.success) {
          alert(result.message);
          history.pushState({ view: "login" }, "", getFullPath("login"));
          showView("login");
        } else {
          alert("Registration failed: " + result.message);
        }
      } catch (error) {
        alert("Registration error: " + error.message);
      }
    });
  }

  document.querySelectorAll(".nav-link").forEach(function (link) {
    link.addEventListener("click", async function (e) {
      e.preventDefault();
      const href = this.getAttribute("href");
      console.log("Link clicked with href:", href);

      let route;
      if (href.includes(CONTEXTPATH)) {
        route = href.replace(CONTEXTPATH, "").replace(/^\//, "");
      } else if (href.startsWith("/")) {
        route = href.substring(1);
      } else {
        route = href;
      }

      console.log(
        "Navigating to route:",
        route,
        "with full path:",
        getFullPath(route)
      );

      history.pushState({ view: route }, "", getFullPath(route));
      await showViewAsync(route);
      setTimeout(showHeroContent, 10);
    });
  });

  window.addEventListener("popstate", async function (e) {
    console.log("Popstate event triggered", e.state);
    const state = e.state;

    if (state && state.view) {
      console.log("Showing view from state:", state.view);
      await showViewAsync(state.view);

      if (state.view === "admin" && isUserAdmin && adminCenterId) {
        initAdminPage(adminCenterId);
      }
    } else {
      const currentView = getRouteFromPath(window.location.pathname);
      console.log("No state, derived view from path:", currentView);
      await showViewAsync(currentView);
    }

    const currentView =
      state?.view || getRouteFromPath(window.location.pathname);

    if (currentView === "appointments") {
      populateCenters();
      updateBookingsCountDisplay();
    } else if (currentView === "my-bookings") {
      initBookingsPage();
    }

    setTimeout(showHeroContent, 10);
  });

  const loginForm = document.getElementById("loginForm");
  if (loginForm) {
    loginForm.addEventListener("submit", async function (e) {
      e.preventDefault();
      const email = document.getElementById("loginEmail").value;
      const password = document.getElementById("loginPassword").value;
      try {
        const result = await loginUser(email, password);
        if (result.success) {
          isUserAdmin = result.isAdmin === true;
          if (isUserAdmin && result.centerId) {
            adminCenterId = result.centerId;
          }

          updateNavbarForAdmin();

          alert("Anmeldung erfolgreich!");
          updateNavbarBasedOnSession();

          if (isUserAdmin) {
            history.pushState({ view: "admin" }, "", getFullPath("admin"));
            showView("admin");
            initAdminPage(adminCenterId);
          } else {
            history.pushState(
              { view: "appointments" },
              "",
              getFullPath("appointments")
            );
            showView("appointments");
            populateCenters();
          }

          updateBookingsCountDisplay();
        } else {
          alert("Login failed: " + result.message);
        }
      } catch (error) {
        alert("Anmeldung fehlgeschlagen: " + error.message);
      }
    });
  }

  document.getElementById("logout")?.addEventListener("click", async (e) => {
    e.preventDefault();
    try {
      const response = await fetch(getApiUrl("logout"), {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
      });
      const result = await response.json();
      if (result.success) {
        isUserAdmin = false;
        adminCenterId = null;

        alert(result.message);
        updateNavbarBasedOnSession();

        const adminLink = document.querySelector(".nav-link.admin-link");
        if (adminLink) adminLink.remove();

        history.pushState({ view: "login" }, "", getFullPath("login"));
        showView("login");
      } else {
        alert("Logout failed: " + result.message);
      }
    } catch (error) {
      alert("Logout error: " + error.message);
    }
  });

  const qrCodeDisplay = document.getElementById("qr-code-display");
  if (qrCodeDisplay) {
    qrCodeDisplay.addEventListener("click", function (e) {
      if (e.target === this) {
        this.style.display = "none";
      }
    });
  }
});
