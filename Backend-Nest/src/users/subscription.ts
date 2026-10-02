import { BadRequestException, ForbiddenException, HttpException, HttpStatus } from '@nestjs/common';

// Simulated payment processor. It validates a card the way a real one would (Luhn checksum,
// expiry, CVC) and follows Stripe's test-card convention - 4000 0000 0000 0002 is always
// declined - but no money moves and only the brand + last 4 digits are ever stored.
// ponytail: no billing cycle - premium lasts until the user cancels. Swap chargeCard() for a
// real provider (Stripe PaymentIntent) and add renewal if this ever takes real payments.

export const PREMIUM_PRICE = { amount: 4.99, currency: 'EUR', interval: 'month' } as const;
export const DECLINED_TEST_CARD = '4000000000000002';

export interface CardInput {
  number: string;
  expMonth: number;
  expYear: number;
  cvc: string;
  holderName: string;
}

export function luhnValid(digits: string) {
  let sum = 0;
  for (let i = 0; i < digits.length; i++) {
    let d = Number(digits[digits.length - 1 - i]);
    if (i % 2 === 1) {
      d *= 2;
      if (d > 9) d -= 9;
    }
    sum += d;
  }
  return sum % 10 === 0;
}

export function cardBrand(digits: string) {
  if (/^4/.test(digits)) return 'Visa';
  if (/^(5[1-5]|2(2[2-9]|[3-6]\d|7[01]|720))/.test(digits)) return 'Mastercard';
  if (/^3[47]/.test(digits)) return 'Amex';
  return 'Card';
}

/** Validates and "charges" the card; returns what is safe to store. */
export function chargeCard(card: CardInput, now = new Date()) {
  const digits = card.number.replace(/[\s-]/g, '');
  if (!/^\d{13,19}$/.test(digits) || !luhnValid(digits)) {
    throw new BadRequestException('Card number is invalid');
  }
  // Card is valid through the last day of its expiry month.
  if (new Date(card.expYear, card.expMonth, 1) <= now) {
    throw new BadRequestException('Card has expired');
  }
  const brand = cardBrand(digits);
  if (!new RegExp(brand === 'Amex' ? '^\\d{4}$' : '^\\d{3}$').test(card.cvc)) {
    throw new BadRequestException('Security code (CVC) is invalid');
  }
  if (digits === DECLINED_TEST_CARD) {
    throw new HttpException('Your card was declined', HttpStatus.PAYMENT_REQUIRED);
  }
  return { brand, last4: digits.slice(-4) };
}

export function assertPremium(user: { isPremium: boolean }, feature: string) {
  if (!user.isPremium) {
    throw new ForbiddenException(`${feature} are a Premium feature. Upgrade from your profile page.`);
  }
}
