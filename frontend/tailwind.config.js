/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ["class"],
  content: [
    './components/**/*.{js,ts,jsx,tsx,mdx}',
    './app/**/*.{js,ts,jsx,tsx,mdx}',
    './pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  prefix: "",
  theme: {
    container: {
      center: true,
      padding: "2rem",
      screens: {
        "2xl": "1400px",
      },
    },
    extend: {
      colors: {
        border: {
          DEFAULT: 'hsl(var(--border))',
          soft: 'hsl(var(--soft))',
          hard: 'hsl(var(--hard))',
        },
        soft: 'hsl(var(--soft))',
        hard: 'hsl(var(--hard))',
        input: 'hsl(var(--input))',
        ring: 'hsl(var(--ring))',
        background: 'hsl(var(--background))',
        foreground: 'hsl(var(--foreground))',
        primary: {
          DEFAULT: 'hsl(var(--primary))',
          foreground: 'hsl(var(--primary-foreground))',
        },
        secondary: {
          DEFAULT: 'hsl(var(--secondary))',
          foreground: 'hsl(var(--secondary-foreground))',
        },
        tertiary: {
          DEFAULT: 'hsl(var(--tertiary))',
          foreground: 'hsl(var(--tertiary-foreground))',
        },
        quaternary: {
          DEFAULT: 'hsl(var(--quaternary))',
          foreground: 'hsl(var(--quaternary-foreground))',
        },
        destructive: {
          DEFAULT: 'hsl(var(--destructive))',
          foreground: 'hsl(var(--destructive-foreground))',
        },
        muted: {
          DEFAULT: 'hsl(var(--muted))',
          foreground: 'hsl(var(--muted-foreground))',
        },
        accent: {
          DEFAULT: 'hsl(var(--accent))',
          foreground: 'hsl(var(--accent-foreground))',
        },
        popover: {
          DEFAULT: 'hsl(var(--popover))',
          foreground: 'hsl(var(--popover-foreground))',
        },
        card: {
          DEFAULT: 'hsl(var(--card))',
          foreground: 'hsl(var(--card-foreground))',
        },
        brand: {
          DEFAULT: 'hsl(var(--brand))',
          foreground: 'hsl(var(--brand-foreground))',
        },
      },
      borderWidth: {
        DEFAULT: '0.8px',
      },
      borderRadius: {
        lg: 'var(--radius)',
        md: 'calc(var(--radius) - 2px)',
        sm: 'calc(var(--radius) - 4px)',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        clash: ['ClashGrotesk', 'system-ui', 'sans-serif'],
      },
      fontSize: {
        xs: ['0.725rem', { lineHeight: '1.2rem', letterSpacing: '0.01em' }],
        sm: ['0.775rem', { lineHeight: '1.3rem', letterSpacing: '0.008em' }],
        base: ['0.875rem', { lineHeight: '1.5rem' }],
        lg: ['0.975rem', { lineHeight: '1.75rem' }],
        xl: ['1.175rem', { lineHeight: '1.95rem' }],
        '2xl': ['1.275rem', { lineHeight: '2.25rem' }],
        '3xl': ['1.375rem', { lineHeight: '2.5rem' }],
        '4xl': ['1.475rem', { lineHeight: '2.75rem' }],
        '5xl': ['3.052rem'],
      },
      fontWeight: {
        normal: '350',
        medium: '400',
        semibold: '450',
        bold: '500',
        black: '600',
      },
      keyframes: {
        "accordion-down": {
          from: { height: "0" },
          to: { height: "var(--radix-accordion-content-height)" },
        },
        "accordion-up": {
          from: { height: "var(--radix-accordion-content-height)" },
          to: { height: "0" },
        },
      },
      animation: {
        "accordion-down": "accordion-down 0.2s ease-out",
        "accordion-up": "accordion-up 0.2s ease-out",
      },
      boxShadow: {
        'subtle-xs': 'var(--shadow-subtle-xs)',
        'subtle-sm': 'var(--shadow-subtle-sm)',
      },
    },
  },
  plugins: [require("tailwindcss-animate")],
}
