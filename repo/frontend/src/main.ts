import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "@/router";
import { setupApiInterceptors } from "@/api";
import { useAuthStore } from "@/stores/auth";
import "@/style.css";

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);

const authStore = useAuthStore(pinia);
authStore.restoreSession();
setupApiInterceptors(authStore);

app.use(router);
app.mount("#app");
