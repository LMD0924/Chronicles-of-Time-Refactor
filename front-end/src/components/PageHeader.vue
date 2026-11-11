<script setup>
defineProps({
  eyebrow: { type: String, default: '' },
  title: { type: String, required: true },
  description: { type: String, default: '' },
  actions: { type: Array, default: () => [] }
})
</script>

<template>
  <div class="mb-7">
    <p v-if="eyebrow" class="eyebrow mb-2">{{ eyebrow }}</p>
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="hand-title display">{{ title }}</h1>
        <p v-if="description" class="mt-2 text-[13px] leading-6 muted">{{ description }}</p>
      </div>
      <div v-if="actions.length" class="btn-group flex flex-wrap gap-2">
        <component :is="a.to ? 'RouterLink' : 'button'" v-for="(a, i) in actions" :key="i"
                   :to="a.to" :type="a.to ? undefined : 'button'" :class="['btn', `btn-${a.color || 'blue'}`]"
                   @click="a.click">
          <span class="btn-glyph">{{ a.icon || '→' }}</span> {{ a.label }}
        </component>
      </div>
    </div>
  </div>
</template>
