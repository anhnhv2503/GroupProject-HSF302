# Culinary Curator: Design System & Semantic Tokens

This document outlines the visual language and semantic design tokens used in the "The Culinary Curator" (SeafoodStoreAI) platform, based on the **Stitch Premium Editorial** design system.

## 🎨 Color Palette

| Token | Value | Role |
| :--- | :--- | :--- |
| `primary` | `#a63b00` | Main brand color. Used for price, primary CTA, and emphasis. |
| `primary-container` | `#f26522` | Soft orange for secondary brand elements and gradients. |
| `secondary` | `#2e6192` | Corporate blue for AI-related components and trust elements. |
| `secondary-container`| `#dae2ff` | Light blue for backgrounds of AI feedback or informative cards. |
| `surface` | `#f8f9fa` | Main page background color. |
| `surface-card` | `#ffffff` | Color for cards, modals, and elevated surfaces. |
| `on-surface` | `#191c1d` | High-contrast dark for primary headings and text. |
| `on-surface-variant`| `#594138` | Muted dark for descriptions and secondary labels. |
| `outline-variant` | `#e1bfb3` | Soft border color for ghost elements. |
| `error` | `#ba1a1a` | Critical alerts and notification badges. |

## 🏗️ Layout & Components

### 👻 Ghost Borders
We use **Ghost Borders** to create a clean, editorial look that emphasizes content over containers.
- **Normal**: `1.5px solid rgba(141,113,102,0.12)`
- **Hover**: `1.5px solid rgba(141,113,102,0.25)`

### 🏷️ Catch Badges
Dynamic labels applied to products to communicate status at a glance.
- **Tươi rói**: Using `--badge-fresh` (Emerald Green).
- **Hàng Hot / Bán chạy**: Using `--badge-hot` (Orange).
- **Cấp đông**: Using `--badge-frozen` (Marine Blue).

### 🖋️ Typography (Editorial Style)
The system prioritizes bold, large headings with tighter letter-spacing for a premium magazine feel.
- **Font-Family**: `'Inter'`, sans-serif.
- **Hero Headings**: `font-weight: 900`, `letter-spacing: -0.04em`.
- **Labels**: `font-weight: 800`, `text-transform: uppercase`.

## ⚙️ Spacing & Motion
- **Radius**: Large border-radius for containers (`16px`, `28px`) for a soft, modern feel.
- **Shadows**: Subtle, wide-area shadows (`shadow-card`) to prevent visual clutter.
- **Motion**: Every interactive card should have a `translateY(-6px)` hover effect and a staggered entrance animation using `cubic-bezier`.
