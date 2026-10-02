import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import { IsIn, IsInt, IsNotEmpty, IsOptional, IsString, Max, Min, ValidateNested } from 'class-validator';

export class PaymentCardDto {
  @ApiProperty({ example: '4242 4242 4242 4242' })
  @IsString()
  number: string;

  @ApiProperty({ example: 12 })
  @IsInt()
  @Min(1)
  @Max(12)
  expMonth: number;

  @ApiProperty({ example: 2030 })
  @IsInt()
  expYear: number;

  @ApiProperty({ example: '123' })
  @IsString()
  cvc: string;

  @ApiProperty({ example: 'Jane Doe' })
  @IsString()
  @IsNotEmpty()
  holderName: string;
}

export class SubscribeDto {
  @ApiProperty({ enum: ['free', 'premium'], example: 'premium' })
  @IsIn(['free', 'premium'])
  subscriptionType: 'free' | 'premium';

  @ApiPropertyOptional({ type: PaymentCardDto, description: 'Required to upgrade to premium' })
  @IsOptional()
  @ValidateNested()
  @Type(() => PaymentCardDto)
  card?: PaymentCardDto;
}
