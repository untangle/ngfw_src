<template>
    <div>
      <h1>{{ title }}</h1>
      <p>{{ message }}</p>
    </div>
  </template>
  
  <script>
  export default {
    data() {
      return {
        title: 'Hello from Vue',
        message: 'This is a Vue component loaded into an ExtJS component.'
      };
    }
  };
  </script>
  
  <style scoped>
  h1 {
    color: blue;
  }
  </style>