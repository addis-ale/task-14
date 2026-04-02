import { ref, computed } from 'vue';
import zhCN from './zh-CN';
import en from './en';

export type Locale = 'zh-CN' | 'en';

const messages: Record<Locale, Record<string, any>> = {
  'zh-CN': zhCN,
  en,
};

const currentLocale = ref<Locale>(
  (localStorage.getItem('locale') as Locale) || 'zh-CN',
);

export function useI18n() {
  const locale = computed(() => currentLocale.value);

  function t(key: string): string {
    const keys = key.split('.');
    let result: any = messages[currentLocale.value];
    for (const k of keys) {
      if (result && typeof result === 'object' && k in result) {
        result = result[k];
      } else {
        return key;
      }
    }
    return typeof result === 'string' ? result : key;
  }

  function setLocale(loc: Locale) {
    currentLocale.value = loc;
    localStorage.setItem('locale', loc);
  }

  return { locale, t, setLocale };
}
