# UI/UX Integration (The Culinary Curator Design System) Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Apply "The Culinary Curator" Stitch design system across all remaining Thymeleaf templates to establish a premium, editorial e-commerce experience.

**Architecture:** We will systematically update existing HTML templates. This includes adding the Roboto font for prices/values, applying glassmorphism and tonal layering to the authentication, cart, checkout, and admin pages. We will enforce "No-Line" sectioning by using background color shifts and removing hard borders.

**Tech Stack:** HTML5, CSS (Custom Design System Variables), Bootstrap 5, Thymeleaf, Spring Boot 3

---

### Task 1: Update Base Layout Typography

**Files:**
- Modify: `src/main/resources/templates/layout/base.html`

**Step 1: Write the failing test**
Run the application and verify if the Roboto font is loaded in the network tab.
Run: `mvn spring-boot:run`
Expected: Only Inter font is loaded.

**Step 2: Write minimal implementation**
```html
  <!-- Modify head to import Roboto -->
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800;900&family=Roboto:wght@400;500;700;900&display=swap" rel="stylesheet"/>
```
And add CSS classes:
```css
  .font-roboto { font-family: 'Roboto', sans-serif; }
```

**Step 3: Run test to verify it passes**
Run: `mvn spring-boot:run`
Navigate to `/` and inspect network for Roboto.
Expected: PASS

**Step 4: Commit**
```bash
git add src/main/resources/templates/layout/base.html
git commit -m "feat(ui): add Roboto typography to base layout"
```

---

### Task 2: Apply Design System to Auth Pages

**Files:**
- Modify: `src/main/resources/templates/auth/login.html`
- Modify: `src/main/resources/templates/auth/register.html`

**Step 1: Write the failing test**
Inspect `http://localhost:8080/login`
Expected: Standard bootstrap form without premium padding, glassmorphic headers, or tonal layering.

**Step 2: Write minimal implementation**
Apply `surface-card` and `shadow-ambient` to form containers. Remove `1px solid` input borders replacing with ghost borders `outline-variant` @ 15%. Apply gradient primary buttons (`btn-culinary-primary`).
```html
<div class="card border-0" style="background: var(--surface-card); box-shadow: var(--shadow-ambient); border-radius: var(--border-radius-xl); padding: 2rem;">
  <!-- Inputs with .input-culinary ... -->
</div>
```

**Step 3: Run test to verify it passes**
Navigate to `/login` and `/register`.
Expected: Forms have premium soft shadows, ghost borders, and Inter font structure. 

**Step 4: Commit**
```bash
git add src/main/resources/templates/auth/login.html src/main/resources/templates/auth/register.html
git commit -m "feat(ui): apply culinary curator design to auth pages"
```

---

### Task 3: Overhaul Cart Page

**Files:**
- Modify: `src/main/resources/templates/cart/cart.html`

**Step 1: Write the failing test**
Navigate to `http://localhost:8080/cart`.
Expected: Cart items use standard grid and dividers.

**Step 2: Write minimal implementation**
Remove dividers between cart items. Use alternating `surface-container-low` vs `surface-card` backgrounds for items. Format prices using `.font-roboto` and label weights. Style standard buttons to `btn-culinary-secondary` and checkout to `btn-culinary-primary`.

**Step 3: Run test to verify it passes**
Add item to cart, navigate to `/cart`.
Expected: Cart items rendered without strict line dividers, values printed in Roboto.

**Step 4: Commit**
```bash
git add src/main/resources/templates/cart/cart.html
git commit -m "feat(ui): apply culinary curator design to cart view"
```

---

### Task 4: Overhaul Checkout Page

**Files:**
- Modify: `src/main/resources/templates/cart/checkout.html`

**Step 1: Write the failing test**
Navigate to `http://localhost:8080/checkout`.
Expected: Simple bootstrap form with solid borders.

**Step 2: Write minimal implementation**
Apply "Intentional Asymmetry". The payment summary should be a `surface-container-low` stacked side-panel. Forms with Ghost Borders. Prices in `.font-roboto`. 

**Step 3: Run test to verify it passes**
Proceed to `/checkout` with an item.
Expected: Order summary and form properly styled with premium padding and typography.

**Step 4: Commit**
```bash
git add src/main/resources/templates/cart/checkout.html
git commit -m "feat(ui): apply culinary curator design to checkout"
```

---

### Task 5: Upgrade Admin Knowledge Pages

**Files:**
- Modify: `src/main/resources/templates/admin/knowledge/list.html`
- Modify: `src/main/resources/templates/admin/knowledge/form.html`

**Step 1: Write the failing test**
Navigate to `http://localhost:8080/admin/knowledge` as Admin.
Expected: Basic bootstrap table with horizontal dividers.

**Step 2: Write minimal implementation**
Remove `table-bordered`. Use `table-borderless`. Alternate row colors instead of lines (`surface` / `surface-container-lowest`). Apply `btn-culinary-primary` to the "Add" button and `btn-culinary-ghost` to "Edit".

**Step 3: Run test to verify it passes**
Visit `/admin/knowledge`.
Expected: Premium dashboard look without thick table border lines.

**Step 4: Commit**
```bash
git add src/main/resources/templates/admin/knowledge/*.html
git commit -m "feat(ui): apply culinary curator design to admin dashboard"
```
