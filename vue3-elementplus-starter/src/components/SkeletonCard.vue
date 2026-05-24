<template>
  <el-skeleton :rows="3" animated class="skeleton-card">
    <template #template>
      <div class="skeleton-card-body">
        <el-skeleton-item variant="image" class="skeleton-img" />
        <div class="skeleton-texts">
          <el-skeleton-item variant="h3" class="skeleton-title" />
          <el-skeleton-item variant="p" class="skeleton-desc" />
          <el-skeleton-item variant="p" class="skeleton-desc short" />
        </div>
      </div>
    </template>
  </el-skeleton>
</template>

<style scoped>
.skeleton-card {
  border-radius: 12px;
  border: 1px solid #e8eaed;
  padding: 20px;
  display: block;
  background: #fff;
}

.skeleton-card-body {
  display: flex;
  gap: 16px;
}

.skeleton-img {
  width: 180px;
  height: 100px;
  flex-shrink: 0;
  border-radius: 8px;
}

.skeleton-texts {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-title {
  width: 60%;
  height: 20px;
  border-radius: 4px;
}

.skeleton-desc {
  width: 100%;
  height: 14px;
  border-radius: 4px;
}

.skeleton-desc.short {
  width: 40%;
}
</style>
