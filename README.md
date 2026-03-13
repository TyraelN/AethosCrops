# Development Tasks

## 1. GUI System

### Tasks
- [ ] Integrate GuiLib into the project
- [ ] Migrate existing GUIs to GuiLib
- [ ] Remove old GUI handling code
- [ ] Test inventory interactions

---

## 2. Crop System Refactor

### Tasks
- [ ] Remove the `CropType` enum
- [ ] Load crops dynamically from the config file
- [ ] Implement config parsing for crop definitions
- [ ] Add validation for invalid or missing entries

---

## 3. Language Refactor

Rename German identifiers to English.

### Examples
- `Krankheit` → `Disease`
- `Käfer` → `Beetle` / `Bug`

### Tasks
- [ ] Search the codebase for remaining German names
- [ ] Update comments and documentation

---

## 4. Command System

### Tasks
- [ ] Replace current command implementation with Command API
- [ ] Refactor command registration
- [ ] Implement argument handling
- [ ] Implement permission checks
- [ ] Add tab completion

---

## 5. Minecraft Version Upgrade

Upgrade the plugin to **Minecraft / Paper 1.21.11**.

### Tasks
- [ ] Update API dependency
- [ ] Fix compilation errors caused by API changes
- [ ] Replace removed or deprecated methods
- [ ] Ensure plugin loads correctly
- [ ] Test gameplay features

---

## 6. Testing

### Tasks
- [ ] Ensure the project compiles successfully
- [ ] Run the plugin on a 1.21.11 test server
