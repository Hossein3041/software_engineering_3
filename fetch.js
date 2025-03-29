import { initBookingsPage } from "./bookingPage.js";
import { initAdminPage } from "./adminPage.js";

let isUserAdmin = false;
let adminCenterId = null;

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

export async function checkAdminStatus() {
  const { getApiUrl } = getRoutingUtils();
  try {
    const response = await fetch(getApiUrl("session/admin"), {
      method: "GET",
      headers: { "Content-Type": "application/json" },
    });
    if (!response.ok) {
      return { isAdmin: false, centerId: null };
    }
    const data = await response.json();
    return {
      isAdmin: data.isAdmin === true,
      centerId: data.centerId,
    };
  } catch (error) {
    console.error("Error checking admin status:", error);
    return { isAdmin: false, centerId: null };
  }
}

export async function loginUser(email, password) {
  const { getApiUrl } = getRoutingUtils();
  try {
    const response = await fetch(getApiUrl("login"), {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ email, password }),
    });
    if (!response.ok) {
      throw new Error("Login failed: " + response.statusText);
    }
    const result = await response.json();

    if (result.isAdmin === true) {
      isUserAdmin = true;
      if (result.centerId) {
        adminCenterId = result.centerId;
      }
    }

    return result;
  } catch (error) {
    console.error("Error during login:", error);
    throw error;
  }
}

export function getAdminStatus() {
  return { isAdmin: isUserAdmin, centerId: adminCenterId };
}

export async function registerUser(registerData) {
  const { getApiUrl } = getRoutingUtils();
  try {
    const response = await fetch(getApiUrl("register"), {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(registerData),
    });
    if (!response.ok) {
      throw new Error("Registration failed: " + response.statusText);
    }
    return await response.json();
  } catch (error) {
    console.error("Error during registration:", error);
    throw error;
  }
}

export async function checkSessionStatus() {
  const { getApiUrl } = getRoutingUtils();
  try {
    const response = await fetch(getApiUrl("session"), {
      method: "GET",
      headers: { "Content-Type": "application/json" },
    });
    const data = await response.json();
    return data.loggedIn;
  } catch (error) {
    console.error("Error checking session status:", error);
    return false;
  }
}

export async function getUserBookingsCount() {
  const { getApiUrl } = getRoutingUtils();
  try {
    const response = await fetch(getApiUrl("my-bookings/count"), {
      method: "GET",
      headers: { "Content-Type": "application/json" },
    });
    if (!response.ok) {
      return 0;
    }
    const data = await response.json();
    return data.count;
  } catch (error) {
    console.error("Error fetching bookings count:", error);
    return 0;
  }
}

export async function updateNavbarBasedOnSession() {
  const isLoggedIn = await checkSessionStatus();

  let adminStatus = { isAdmin: false, centerId: null };
  if (isLoggedIn) {
    adminStatus = await checkAdminStatus();

    isUserAdmin = adminStatus.isAdmin;
    adminCenterId = adminStatus.centerId;
  }

  const loginLink = document.querySelector('.nav-link[href="/login"]');
  const registerLink = document.querySelector('.nav-link[href="/register"]');
  const appointmentsLink = document.querySelector(
    '.nav-link[href="/appointments"]'
  );
  const logoutLink = document.querySelector("#logout");
  const myBookingsLink = document.querySelector("#my-bookings-link");
  const adminLink = document.querySelector(".nav-link.admin-link");

  if (isLoggedIn) {
    if (loginLink) loginLink.style.display = "none";
    if (registerLink) registerLink.style.display = "none";
    if (logoutLink) logoutLink.style.display = "block";
    if (appointmentsLink)
      appointmentsLink.style.display = adminStatus.isAdmin ? "none" : "block";
    if (myBookingsLink)
      myBookingsLink.style.display = adminStatus.isAdmin ? "none" : "block";

    if (adminStatus.isAdmin) {
      if (!adminLink) {
        const navbar = document.querySelector(".nav");
        const newAdminLink = document.createElement("a");
        newAdminLink.href = "/admin";
        newAdminLink.className = "nav-link admin-link";
        newAdminLink.textContent = "Admin-Bereich";
        newAdminLink.style.color = "#ffeb3b";

        navbar.insertBefore(newAdminLink, logoutLink);

        newAdminLink.addEventListener("click", function (e) {
          e.preventDefault();
          window.history.pushState({ view: "admin" }, "", "/admin");
          showView("admin");
        });
      } else {
        adminLink.style.display = "block";
      }
    } else {
      if (adminLink) {
        adminLink.style.display = "none";
      }
    }

    updateBookingsCountDisplay();
  } else {
    if (loginLink) loginLink.style.display = "block";
    if (registerLink) registerLink.style.display = "block";
    if (logoutLink) logoutLink.style.display = "none";
    if (appointmentsLink) appointmentsLink.style.display = "none";
    if (myBookingsLink) myBookingsLink.style.display = "none";
    if (adminLink) adminLink.style.display = "none";

    isUserAdmin = false;
    adminCenterId = null;
  }

  return adminStatus;
}

export async function updateBookingsCountDisplay() {
  const countElement = document.querySelector("#current-bookings-count span");
  if (countElement) {
    const count = await getUserBookingsCount();
    countElement.textContent = count;
  }
}

export async function fetchCenters() {
  const { getApiUrl } = getRoutingUtils();
  try {
    const response = await fetch(getApiUrl("centers"), {
      method: "GET",
      headers: { "Content-Type": "application/json" },
    });
    if (!response.ok) {
      throw new Error(
        "Fehler beim Laden der Impfzentren: " + response.statusText
      );
    }
    return await response.json();
  } catch (error) {
    console.error("Error fetching centers:", error);
    return [];
  }
}

export async function fetchSlots(centerId, date) {
  const { getApiUrl } = getRoutingUtils();
  try {
    let url = getApiUrl(`slots?centerId=${centerId}`);
    if (date) {
      url += `&date=${date}`;
    }
    console.log("url", url);
    const response = await fetch(url, {
      method: "GET",
      headers: { "Content-Type": "application/json" },
    });
    if (!response.ok) {
      const err = await response.json();
      console.log("err", err);
      throw new Error("Fehler beim Laden der Zeitslots: " + err);
    }
    const ans = await response.json();
    if (ans.length > 0) {
      return ans;
    } else {
      alert("Keine Zeitslots für dieses Datum verfügbar");
    }
    return [];
  } catch (error) {
    console.error("Error fetching slots:", error);
    return [];
  }
}

export async function fetchVaccines(centerId) {
  const { getApiUrl } = getRoutingUtils();
  try {
    const response = await fetch(getApiUrl(`centers/${centerId}/vaccines`), {
      method: "GET",
      headers: { "Content-Type": "application/json" },
    });
    if (!response.ok) {
      throw new Error(
        "Fehler beim Laden der Impfstoffe: " + response.statusText
      );
    }
    return await response.json();
  } catch (error) {
    console.error("Error fetching vaccines:", error);
    return [];
  }
}

export async function populateVaccines(centerId) {
  const vaccines = await fetchVaccines(centerId);
  const vaccineSelect = document.getElementById("vaccineSelect");
  vaccineSelect.innerHTML = "<option value=''>Bitte wählen</option>";
  vaccines.forEach((vaccine) => {
    const option = document.createElement("option");
    option.value = vaccine.id;
    option.textContent = vaccine.name;
    vaccineSelect.appendChild(option);
  });
}

async function checkViewAuthorization(viewId) {
  const { getFullPath } = getRoutingUtils();
  const allowedViews = ["hero", "login", "register"];
  const protectedViews = ["appointments", "my-bookings"];
  const adminViews = ["admin"];

  if (protectedViews.includes(viewId)) {
    const loggedIn = await checkSessionStatus();
    if (!loggedIn) {
      history.replaceState({ view: "login" }, "", getFullPath("login"));
      return "login";
    }
  }

  if (adminViews.includes(viewId)) {
    const loggedIn = await checkSessionStatus();

    if (!loggedIn) {
      history.replaceState({ view: "login" }, "", getFullPath("login"));
      return "login";
    }

    const adminStatus = await checkAdminStatus();
    if (!adminStatus.isAdmin) {
      history.replaceState(
        { view: "appointments" },
        "",
        getFullPath("appointments")
      );
      return "appointments";
    }

    isUserAdmin = true;
    adminCenterId = adminStatus.centerId;
  }

  return viewId;
}

export async function showViewAsync(viewId) {
  const authorizedView = await checkViewAuthorization(viewId);
  showView(authorizedView);

  if (authorizedView === "my-bookings") {
    initBookingsPage();
  }

  if (authorizedView === "appointments") {
    updateBookingsCountDisplay();
  }

  if (authorizedView === "admin" && isUserAdmin && adminCenterId) {
    initAdminPage(adminCenterId);
  }
}

export function showView(viewId) {
  const views = document.querySelectorAll(".view");
  views.forEach((view) => view.classList.remove("active"));
  const activeView = document.getElementById(viewId);
  console.log("Showing view:", viewId, activeView);
  if (activeView) {
    activeView.classList.add("active");
  }
}
